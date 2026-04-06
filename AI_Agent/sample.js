import { GoogleGenerativeAI } from "@google/generative-ai";
import readlineSync from 'readline-sync';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

// --- CONFIGURATION ---
const GEMINI_API_KEY = "API_KEY_HERE"; 
const DATA_DIR = './data';
const TASK_FILE = join(DATA_DIR, 'tasks.json');
const HISTORY_FILE = join(DATA_DIR, 'history.json');

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
}

// --- UTILITY FUNCTIONS ---

function loadTasks() {
    if (!fs.existsSync(TASK_FILE)) return [];
    try {
        return JSON.parse(fs.readFileSync(TASK_FILE, 'utf8'));
    } catch (error) {
        console.error('⚠️  Error loading tasks, creating new file');
        return [];
    }
}

function saveTasks(tasks) {
    fs.writeFileSync(TASK_FILE, JSON.stringify(tasks, null, 2));
}

function loadHistory() {
    if (!fs.existsSync(HISTORY_FILE)) return [];
    try {
        return JSON.parse(fs.readFileSync(HISTORY_FILE, 'utf8'));
    } catch (error) {
        return [];
    }
}

function saveHistory(history) {
    // Keep only last 100 entries
    const trimmed = history.slice(-100);
    fs.writeFileSync(HISTORY_FILE, JSON.stringify(trimmed, null, 2));
}

function logAction(action, details) {
    const history = loadHistory();
    history.push({
        timestamp: new Date().toISOString(),
        action,
        details
    });
    saveHistory(history);
}

// --- CORE TASK FUNCTIONS ---

function addTask({ description, status = "pending", priority = "medium", due_date = null, tags = [] }) {
    const tasks = loadTasks();
    const now = new Date().toISOString();
    const newTask = {
        id: tasks.length > 0 ? Math.max(...tasks.map(t => t.id)) + 1 : 1,
        description,
        status,
        priority,
        due_date,
        tags: Array.isArray(tags) ? tags : [],
        created_at: now,
        updated_at: now
    };
    tasks.push(newTask);
    saveTasks(tasks);
    logAction('ADD_TASK', newTask);
    return { 
        result: `✅ Task added (ID: ${newTask.id}, Priority: ${priority})`,
        task: newTask 
    };
}

function listTasks({ filter = "all", priority = null, tag = null, sort_by = "created" }) {
    let tasks = loadTasks();
    
    // Apply filters
    if (filter !== "all") {
        tasks = tasks.filter(t => t.status === filter);
    }
    if (priority) {
        tasks = tasks.filter(t => t.priority === priority);
    }
    if (tag) {
        tasks = tasks.filter(t => t.tags && t.tags.includes(tag));
    }
    
    // Sort tasks
    if (sort_by === "priority") {
        const priorityOrder = { high: 1, medium: 2, low: 3 };
        tasks.sort((a, b) => priorityOrder[a.priority] - priorityOrder[b.priority]);
    } else if (sort_by === "due_date") {
        tasks.sort((a, b) => {
            if (!a.due_date) return 1;
            if (!b.due_date) return -1;
            return new Date(a.due_date) - new Date(b.due_date);
        });
    }
    
    if (tasks.length === 0) return { result: "No tasks found." };
    
    return { 
        result: `Found ${tasks.length} task(s)`,
        tasks: tasks.map(t => ({
            id: t.id,
            description: t.description,
            status: t.status,
            priority: t.priority,
            due_date: t.due_date,
            tags: t.tags,
            created: t.created_at
        }))
    };
}

function updateTask({ task_id, description = null, status = null, priority = null, due_date = null, tags = null }) {
    const tasks = loadTasks();
    const taskIndex = tasks.findIndex(t => t.id == task_id);
    
    if (taskIndex === -1) {
        return { error: "Task not found." };
    }
    
    const task = tasks[taskIndex];
    if (description !== null) task.description = description;
    if (status !== null) task.status = status;
    if (priority !== null) task.priority = priority;
    if (due_date !== null) task.due_date = due_date;
    if (tags !== null) task.tags = Array.isArray(tags) ? tags : [];
    task.updated_at = new Date().toISOString();
    
    if (status === "completed" && !task.completed_at) {
        task.completed_at = new Date().toISOString();
    }
    
    saveTasks(tasks);
    logAction('UPDATE_TASK', task);
    return { 
        result: `✅ Task ${task_id} updated successfully`,
        task 
    };
}

function deleteTask({ task_id }) {
    const tasks = loadTasks();
    const taskIndex = tasks.findIndex(t => t.id == task_id);
    
    if (taskIndex === -1) {
        return { error: "Task not found." };
    }
    
    const deletedTask = tasks[taskIndex];
    tasks.splice(taskIndex, 1);
    saveTasks(tasks);
    logAction('DELETE_TASK', deletedTask);
    return { result: `🗑️  Task ${task_id} deleted: "${deletedTask.description}"` };
}

function searchTasks({ query }) {
    const tasks = loadTasks();
    const lowerQuery = query.toLowerCase();
    const matches = tasks.filter(t => 
        t.description.toLowerCase().includes(lowerQuery) ||
        (t.tags && t.tags.some(tag => tag.toLowerCase().includes(lowerQuery)))
    );
    
    if (matches.length === 0) {
        return { result: `No tasks found matching "${query}"` };
    }
    
    return {
        result: `Found ${matches.length} matching task(s)`,
        tasks: matches.map(t => ({
            id: t.id,
            description: t.description,
            status: t.status,
            priority: t.priority
        }))
    };
}

function getStatistics() {
    const tasks = loadTasks();
    const total = tasks.length;
    const completed = tasks.filter(t => t.status === "completed").length;
    const pending = tasks.filter(t => t.status === "pending").length;
    const highPriority = tasks.filter(t => t.priority === "high" && t.status === "pending").length;
    
    const overdue = tasks.filter(t => {
        if (!t.due_date || t.status === "completed") return false;
        return new Date(t.due_date) < new Date();
    }).length;
    
    const allTags = tasks.flatMap(t => t.tags || []);
    const tagCounts = allTags.reduce((acc, tag) => {
        acc[tag] = (acc[tag] || 0) + 1;
        return acc;
    }, {});
    
    return {
        result: "📊 Task Statistics",
        stats: {
            total,
            completed,
            pending,
            high_priority: highPriority,
            overdue,
            completion_rate: total > 0 ? `${((completed / total) * 100).toFixed(1)}%` : "0%",
            popular_tags: Object.entries(tagCounts).sort((a, b) => b[1] - a[1]).slice(0, 5)
        }
    };
}

function getUpcomingDeadlines({ days = 7 }) {
    const tasks = loadTasks();
    const now = new Date();
    const futureDate = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
    
    const upcoming = tasks.filter(t => {
        if (!t.due_date || t.status === "completed") return false;
        const dueDate = new Date(t.due_date);
        return dueDate >= now && dueDate <= futureDate;
    }).sort((a, b) => new Date(a.due_date) - new Date(b.due_date));
    
    if (upcoming.length === 0) {
        return { result: `No tasks due in the next ${days} days` };
    }
    
    return {
        result: `⏰ ${upcoming.length} task(s) due in the next ${days} days`,
        tasks: upcoming.map(t => ({
            id: t.id,
            description: t.description,
            due_date: t.due_date,
            priority: t.priority,
            days_until_due: Math.ceil((new Date(t.due_date) - now) / (1000 * 60 * 60 * 24))
        }))
    };
}

function exportTasks({ format = "json" }) {
    const tasks = loadTasks();
    const timestamp = new Date().toISOString().split('T')[0];
    const filename = `tasks_export_${timestamp}.${format}`;
    
    if (format === "csv") {
        const headers = "ID,Description,Status,Priority,Due Date,Tags,Created At\n";
        const rows = tasks.map(t => 
            `${t.id},"${t.description}",${t.status},${t.priority},${t.due_date || ''},"${(t.tags || []).join(';')}",${t.created_at}`
        ).join('\n');
        fs.writeFileSync(filename, headers + rows);
    } else {
        fs.writeFileSync(filename, JSON.stringify(tasks, null, 2));
    }
    
    return { result: `📤 Tasks exported to ${filename}` };
}

// --- FUNCTION REGISTRY ---

const functions = {
    addTask,
    listTasks,
    updateTask,
    deleteTask,
    searchTasks,
    getStatistics,
    getUpcomingDeadlines,
    exportTasks
};

// --- TOOL DEFINITIONS FOR GEMINI ---

const toolsDefinition = [{
    functionDeclarations: [
        {
            name: "addTask",
            description: "Adds a new task with optional priority, due date, and tags",
            parameters: {
                type: "OBJECT",
                properties: {
                    description: { type: "STRING", description: "Task description" },
                    status: { type: "STRING", enum: ["pending", "completed"], description: "Initial status" },
                    priority: { type: "STRING", enum: ["low", "medium", "high"], description: "Task priority" },
                    due_date: { type: "STRING", description: "Due date in ISO format (YYYY-MM-DD)" },
                    tags: { type: "ARRAY", items: { type: "STRING" }, description: "Tags for categorization" }
                },
                required: ["description"]
            }
        },
        {
            name: "listTasks",
            description: "Lists tasks with filtering and sorting options",
            parameters: {
                type: "OBJECT",
                properties: {
                    filter: { type: "STRING", enum: ["all", "pending", "completed"], description: "Status filter" },
                    priority: { type: "STRING", enum: ["low", "medium", "high"], description: "Priority filter" },
                    tag: { type: "STRING", description: "Filter by specific tag" },
                    sort_by: { type: "STRING", enum: ["created", "priority", "due_date"], description: "Sort order" }
                }
            }
        },
        {
            name: "updateTask",
            description: "Updates an existing task's properties",
            parameters: {
                type: "OBJECT",
                properties: {
                    task_id: { type: "NUMBER", description: "Task ID to update" },
                    description: { type: "STRING", description: "New description" },
                    status: { type: "STRING", enum: ["pending", "completed"] },
                    priority: { type: "STRING", enum: ["low", "medium", "high"] },
                    due_date: { type: "STRING", description: "New due date (YYYY-MM-DD)" },
                    tags: { type: "ARRAY", items: { type: "STRING" }, description: "New tags" }
                },
                required: ["task_id"]
            }
        },
        {
            name: "deleteTask",
            description: "Permanently deletes a task",
            parameters: {
                type: "OBJECT",
                properties: {
                    task_id: { type: "NUMBER", description: "Task ID to delete" }
                },
                required: ["task_id"]
            }
        },
        {
            name: "searchTasks",
            description: "Searches tasks by keyword in description or tags",
            parameters: {
                type: "OBJECT",
                properties: {
                    query: { type: "STRING", description: "Search keyword" }
                },
                required: ["query"]
            }
        },
        {
            name: "getStatistics",
            description: "Shows task statistics and analytics",
            parameters: { type: "OBJECT" }
        },
        {
            name: "getUpcomingDeadlines",
            description: "Shows tasks due soon",
            parameters: {
                type: "OBJECT",
                properties: {
                    days: { type: "NUMBER", description: "Number of days to look ahead (default 7)" }
                }
            }
        },
        {
            name: "exportTasks",
            description: "Exports all tasks to a file",
            parameters: {
                type: "OBJECT",
                properties: {
                    format: { type: "STRING", enum: ["json", "csv"], description: "Export format" }
                }
            }
        }
    ]
}];

// --- MAIN AGENT LOOP ---

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
const model = genAI.getGenerativeModel({
    model: "gemini-2.5-flash",
    tools: toolsDefinition,
});

const systemPrompt = `You are an intelligent task management assistant. 

Key capabilities:
- Add tasks with priorities (low/medium/high), due dates, and tags
- Update, delete, and search tasks
- Provide statistics and upcoming deadline alerts
- Export tasks to JSON or CSV

Be proactive: suggest priorities for urgent tasks, warn about overdue items, and recommend organizing tasks with tags. When users ask vague questions like "what should I do?", check their pending tasks and suggest high-priority or overdue items first.`;

const chat = model.startChat({
    history: [
        {
            role: "user",
            parts: [{ text: systemPrompt }]
        },
        {
            role: "model",
            parts: [{ text: "I'm ready to help you manage your tasks efficiently. I can add tasks with priorities and deadlines, search and organize them, and provide insights on your productivity." }]
        }
    ]
});

async function run() {
    console.log("\n╔═══════════════════════════════════════╗");
    console.log("║   🚀 Enhanced Task Management Agent   ║");
    console.log("╚═══════════════════════════════════════╝\n");
    console.log("Commands: Type naturally or use 'help', 'stats', 'exit'\n");
    
    while (true) {
        const query = readlineSync.question('You: ');
        
        if (query.toLowerCase() === "exit") {
            console.log("\n👋 Goodbye! Stay productive!\n");
            break;
        }
        
        if (query.toLowerCase() === "help") {
            console.log(`
📖 Available Commands:
   • Add tasks: "add task: finish report"
   • With priority: "add high priority task: urgent meeting"
   • With deadline: "add task due tomorrow: submit proposal"
   • List tasks: "show my pending tasks", "list all"
   • Update: "mark task 3 as completed", "change task 2 priority to high"
   • Search: "find tasks about meeting"
   • Delete: "delete task 5"
   • Statistics: "show stats"
   • Deadlines: "what's due this week?"
   • Export: "export tasks to CSV"
            `);
            continue;
        }

        try {
            let result = await chat.sendMessage(query);
            let response = result.response;
            
            // Function calling loop
            while (response.functionCalls && response.functionCalls()) {
                const calls = response.functionCalls();
                const functionResponses = [];

                for (const call of calls) {
                    const fnName = call.name;
                    const fnArgs = call.args;

                    console.log(`\n⚙️  ${fnName}(${JSON.stringify(fnArgs, null, 2)})`);

                    const fnResult = functions[fnName](fnArgs);
                    console.log(`✓ ${fnResult.result || JSON.stringify(fnResult, null, 2)}\n`);

                    functionResponses.push({
                        functionResponse: {
                            name: fnName,
                            response: fnResult
                        }
                    });
                }

                result = await chat.sendMessage(functionResponses);
                response = result.response;
            }

            console.log(`🤖 Assistant: ${response.text()}\n`);

        } catch (error) {
            console.error(`\n❌ Error: ${error.message}\n`);
        }
    }
}

run();
