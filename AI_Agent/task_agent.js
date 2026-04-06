import { GoogleGenerativeAI } from "@google/generative-ai";
import readlineSync from 'readline-sync';
import fs from 'fs';

// --- CONFIGURATION ---
const GEMINI_API_KEY = "API_KEY_HERE"; 
const TASK_FILE = 'tasks.json';

// --- 1. REAL JAVASCRIPT FUNCTIONS ---

function loadTasks() {
    if (!fs.existsSync(TASK_FILE)) return [];
    return JSON.parse(fs.readFileSync(TASK_FILE, 'utf8'));
}

function saveTasks(tasks) {
    fs.writeFileSync(TASK_FILE, JSON.stringify(tasks, null, 2));
}

// Tool 1: Add Task
function addTask({ description, status = "pending" }) {
    const tasks = loadTasks();
    const now = new Date().toLocaleString();
    const newTask = {
        id: tasks.length + 1,
        description,
        status,
        created_at: now
    };
    tasks.push(newTask);
    saveTasks(tasks);
    return { result: `Task added ID:${newTask.id} (${status})` };
}

// Tool 2: List Tasks
function listTasks({ filter = "all" }) {
    const tasks = loadTasks();
    const filtered = filter === "all" ? tasks : tasks.filter(t => t.status === filter);
    
    if (filtered.length === 0) return { result: "No tasks found." };
    
   
    return { 
        tasks: filtered.map(t => ({
            id: t.id, 
            desc: t.description, 
            status: t.status,
            time: t.created_at 
        })) 
    };
}

// Tool 3: Complete Task
function completeTask({ task_id }) {
    const tasks = loadTasks();
    
    const taskIndex = tasks.findIndex(t => t.id == task_id || t.description.includes(task_id));
    
    if (taskIndex !== -1) {
        tasks[taskIndex].status = "completed";
        tasks[taskIndex].completed_at = new Date().toLocaleString();
        saveTasks(tasks);
        return { result: `Task '${tasks[taskIndex].description}' marked completed.` };
    }
    return { error: "Task not found." };
}

// Map function names to actual functions
const functions = {
    addTask: addTask,
    listTasks: listTasks,
    completeTask: completeTask
};

// --- 2. DEFINE TOOLS FOR GEMINI (The Schema) ---
// This tells Gemini exactly what inputs are allowed. No more JSON parsing errors.

const toolsDefinition = [
  {
    functionDeclarations: [
      {
        name: "addTask",
        description: "Adds a new task to the todo list.",
        parameters: {
          type: "OBJECT",
          properties: {
            description: { type: "STRING", description: "The content of the task" },
            status: { type: "STRING", description: "Status: 'pending' or 'completed'", enum: ["pending", "completed"] }
          },
          required: ["description"]
        }
      },
      {
        name: "listTasks",
        description: "Lists current tasks. Use this to find IDs or check status.",
        parameters: {
          type: "OBJECT",
          properties: {
            filter: { type: "STRING", description: "Filter by 'pending', 'completed', or 'all'" }
          }
        }
      },
      {
        name: "completeTask",
        description: "Marks a task as finished.",
        parameters: {
          type: "OBJECT",
          properties: {
            task_id: { type: "STRING", description: "The ID or unique description of the task to complete" }
          },
          required: ["task_id"]
        }
      }
    ]
  }
];

// --- 3. MAIN AGENT LOOP ---

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
const model = genAI.getGenerativeModel({
    model: "gemini-2.5-flash", // Use 1.5-flash for speed and low cost
    tools: toolsDefinition,
});

const chat = model.startChat({
    history: [
        {
            role: "user",
            parts: [{ text: "You are a helpful task manager. If I add a task, confirm it. If I ask what to do, check my list." }]
        },
        {
            role: "model",
            parts: [{ text: "Understood. I am ready to manage your tasks." }]
        }
    ]
});

async function run() {
    console.log("📝 Native Task Agent (Type 'exit' to quit)");
    
    while (true) {
        const query = readlineSync.question('>> ');
        if (query.toLowerCase() === "exit") break;

        try {
            // 1. Send User Input
            let result = await chat.sendMessage(query);
            let response = result.response;
            
            // 2. Loop while the AI wants to call functions (Automatic Function Calling Loop)
            // The API might return a "functionCall" request instead of text.
            while (response.functionCalls()) {
                const calls = response.functionCalls();
                const functionResponses = [];

                for (const call of calls) {
                    const fnName = call.name;
                    const fnArgs = call.args;

                    console.log(`⚙️  AI calling tool: ${fnName}(${JSON.stringify(fnArgs)})`);

                    // Execute actual JS code
                    const fnResult = functions[fnName](fnArgs);
                    
                    console.log(`✅  Tool Result:`, fnResult);

                    // Prepare response for AI
                    functionResponses.push({
                        functionResponse: {
                            name: fnName,
                            response: fnResult
                        }
                    });
                }

                // Send the tool results back to Gemini so it can generate the final answer
                result = await chat.sendMessage(functionResponses);
                response = result.response;
            }

            // 3. Final Text Response
            console.log(`🤖: ${response.text()}`);

        } catch (error) {
            console.error("❌ Error:", error.message);
            // If you hit quota, this will catch it cleanly
        }
    }
}

run();
