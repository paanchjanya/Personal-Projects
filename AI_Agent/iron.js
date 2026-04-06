import { GoogleGenerativeAI } from "@google/generative-ai";
import readlineSync from 'readline-sync';
import fs from 'fs';

// REPLACE WITH YOUR GEMINI API KEY
const GEMINI_API_KEY = "API_KEY_HERE";

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);

// ========== ENHANCED TOOL SYSTEM ==========

// City coordinates database for weather lookup
const cityCoordinates = {
    "vijayapura": { lat: 16.8302, lon: 75.7100, name: "Vijayapura" },
    "bagalkot": { lat: 16.1850, lon: 75.6969, name: "Bagalkot" },
    "bangalore": { lat: 12.9716, lon: 77.5946, name: "Bangalore" },
    "bengaluru": { lat: 12.9716, lon: 77.5946, name: "Bengaluru" },
    "bidar": { lat: 17.9104, lon: 77.5199, name: "Bidar" },
    "mumbai": { lat: 19.0760, lon: 72.8777, name: "Mumbai" },
    "delhi": { lat: 28.7041, lon: 77.1025, name: "Delhi" },
    "chennai": { lat: 13.0827, lon: 80.2707, name: "Chennai" },
    "kolkata": { lat: 22.5726, lon: 88.3639, name: "Kolkata" },
    "hyderabad": { lat: 17.3850, lon: 78.4867, name: "Hyderabad" },
    "pune": { lat: 18.5204, lon: 73.8567, name: "Pune" }
};

// Real weather tool using Open-Meteo API
async function get_weather_details(city) {
    try {
        const cityKey = city.toLowerCase().trim();
        const coords = cityCoordinates[cityKey];
        
        if (!coords) {
            return JSON.stringify({ 
                error: "City not found. Available cities: " + Object.keys(cityCoordinates).join(", ") 
            });
        }

        // Fetch weather data from Open-Meteo API
        const url = `https://api.open-meteo.com/v1/forecast?latitude=${coords.lat}&longitude=${coords.lon}&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto`;
        
        const response = await fetch(url);
        const data = await response.json();
        
        // Weather code to description mapping
        const weatherCodes = {
            0: "Clear sky", 1: "Mainly clear", 2: "Partly cloudy", 3: "Overcast",
            45: "Foggy", 48: "Depositing rime fog",
            51: "Light drizzle", 53: "Moderate drizzle", 55: "Dense drizzle",
            61: "Slight rain", 63: "Moderate rain", 65: "Heavy rain",
            71: "Slight snow", 73: "Moderate snow", 75: "Heavy snow",
            80: "Slight rain showers", 81: "Moderate rain showers", 82: "Violent rain showers",
            95: "Thunderstorm", 96: "Thunderstorm with slight hail", 99: "Thunderstorm with heavy hail"
        };
        
        const current = data.current;
        const weatherDescription = weatherCodes[current.weather_code] || "Unknown";
        
        return JSON.stringify({
            city: coords.name,
            temperature: `${current.temperature_2m}°C`,
            humidity: `${current.relative_humidity_2m}%`,
            condition: weatherDescription,
            wind_speed: `${current.wind_speed_10m} km/h`,
            timestamp: current.time
        });
    } catch (error) {
        return JSON.stringify({ error: `Failed to fetch weather: ${error.message}` });
    }
}

// Calculator tool
function calculate(expression) {
    try {
        // Safe evaluation for basic math
        const result = Function('"use strict"; return (' + expression + ')')();
        return `Result: ${result}`;
    } catch (e) {
        return "Invalid expression";
    }
}

// Web search simulator (in real app, use actual API)
function web_search(query) {
    const mockResults = {
        "ai trends": "Latest AI trends include multimodal models, agent systems, and improved reasoning capabilities.",
        "job market": "Tech job market shows strong demand for AI/ML engineers, with salaries ranging $80k-$150k for entry-level positions.",
        "engineering careers": "Popular engineering career paths include software, data science, AI/ML, and cloud computing."
    };
    
    for (let key in mockResults) {
        if (query.toLowerCase().includes(key)) {
            return mockResults[key];
        }
    }
    return "No specific results found for this query.";
}

// Note-taking system
const notes = [];
function create_note(content) {
    const note = {
        id: notes.length + 1,
        content: content,
        timestamp: new Date().toISOString()
    };
    notes.push(note);
    return `Note created with ID ${note.id}`;
}

function list_notes() {
    if (notes.length === 0) return "No notes found";
    return JSON.stringify(notes.map(n => ({ id: n.id, content: n.content })));
}

function get_note(id) {
    const note = notes.find(n => n.id === parseInt(id));
    return note ? JSON.stringify(note) : "Note not found";
}

// Task management
const tasks = [];
function create_task(description, priority = "medium") {
    const task = {
        id: tasks.length + 1,
        description: description,
        priority: priority,
        status: "pending",
        created: new Date().toISOString()
    };
    tasks.push(task);
    return `Task created with ID ${task.id}`;
}

function list_tasks(status = "all") {
    if (tasks.length === 0) return "No tasks found";
    const filtered = status === "all" ? tasks : tasks.filter(t => t.status === status);
    return JSON.stringify(filtered);
}

function update_task_status(id, status) {
    const task = tasks.find(t => t.id === parseInt(id));
    if (!task) return "Task not found";
    task.status = status;
    return `Task ${id} status updated to ${status}`;
}

// Date and time utilities
function get_current_datetime() {
    const now = new Date();
    return JSON.stringify({
        date: now.toDateString(),
        time: now.toTimeString().split(' ')[0],
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
    });
}

// File operations
function save_to_file(filename, content) {
    try {
        fs.writeFileSync(filename, content);
        return `Content saved to ${filename}`;
    } catch (e) {
        return `Error saving file: ${e.message}`;
    }
}

function read_from_file(filename) {
    try {
        const content = fs.readFileSync(filename, 'utf8');
        return content;
    } catch (e) {
        return `Error reading file: ${e.message}`;
    }
}

// ========== TOOL REGISTRY ==========
const tools = {
    "get_weather_details": get_weather_details,
    "calculate": calculate,
    "web_search": web_search,
    "create_note": create_note,
    "list_notes": list_notes,
    "get_note": get_note,
    "create_task": create_task,
    "list_tasks": list_tasks,
    "update_task_status": update_task_status,
    "get_current_datetime": get_current_datetime,
    "save_to_file": save_to_file,
    "read_from_file": read_from_file
};

const TOOL_DESCRIPTIONS = `
Available Tools:
1. get_weather_details(city) - Get REAL-TIME weather for any Indian city (vijayapura, bagalkot, bangalore, bidar, mumbai, delhi, chennai, kolkata, hyderabad, pune, etc.)
2. calculate(expression) - Perform mathematical calculations (e.g., "5 * 10 + 3")
3. web_search(query) - Search for information online
4. create_note(content) - Create a new note
5. list_notes() - List all notes
6. get_note(id) - Get a specific note by ID
7. create_task(description, priority) - Create a task (priority: low/medium/high)
8. list_tasks(status) - List tasks (status: all/pending/completed)
9. update_task_status(id, status) - Update task status (pending/in_progress/completed)
10. get_current_datetime() - Get current date and time
11. save_to_file(filename, content) - Save content to a file
12. read_from_file(filename) - Read content from a file
`;

// ========== ENHANCED SYSTEM PROMPT ==========
const SYSTEM_PROMPT = `
You are an advanced AI Agent with sophisticated reasoning capabilities. You operate in states: START, PLAN, ACTION, OBSERVATION, and OUTPUT.

${TOOL_DESCRIPTIONS}

CORE PRINCIPLES:
1. MEMORY FIRST: Always check conversation history before calling tools. Reuse existing data.
2. MULTI-STEP REASONING: Break complex tasks into logical steps. Plan before acting.
3. TOOL CHAINING: You can use multiple tools in sequence to accomplish complex tasks.
4. INTERNAL PROCESSING: Perform calculations, conversions, and data transformations yourself when possible.
5. CONTEXTUAL AWARENESS: Remember user preferences and previous interactions.
6. ERROR HANDLING: If a tool fails, explain why and suggest alternatives.

RESPONSE FORMAT (Strict JSON):
{"type": "start", "thought": "Initial analysis of the user's request"}
{"type": "plan", "plan": "Detailed step-by-step plan", "steps": ["step1", "step2"]}
{"type": "action", "function": "tool_name", "input": "parameter"}
{"type": "observation", "observation": "result from tool"}
{"type": "output", "output": "Final answer to user"}

ADVANCED BEHAVIORS:
- For multi-step tasks, create a detailed plan first
- Chain multiple actions together when needed
- Synthesize information from multiple sources
- Provide context and explanations with your answers
- Suggest related actions the user might want to take

Example (Complex Query):
User: "What's the weather in Bangalore and create a reminder to check it again tomorrow"
{"type": "start", "thought": "User wants weather info and task creation - requires two tools"}
{"type": "plan", "plan": "1. Get weather for Bangalore 2. Create task for tomorrow", "steps": ["get_weather", "create_task"]}
{"type": "action", "function": "get_weather_details", "input": "bangalore"}
{"type": "observation", "observation": "..."}
{"type": "action", "function": "create_task", "input": "Check Bangalore weather"}
{"type": "observation", "observation": "..."}
{"type": "output", "output": "The weather in Bangalore is 30°C and partly cloudy. I've created a reminder task for you to check it again."}
`;

// ========== AGENT EXECUTION ENGINE ==========
const model = genAI.getGenerativeModel({
    model: "gemini-2.5-flash",
    systemInstruction: SYSTEM_PROMPT, 
    generationConfig: { responseMimeType: "application/json" }
});

const chat = model.startChat();

// Session memory for context
const sessionContext = {
    userName: null,
    preferences: {},
    conversationCount: 0
};

async function run() {
    console.log("╔════════════════════════════════════════╗");
    console.log("║   Advanced AI Agent System v2.0        ║");
    console.log("║   Type 'help' for commands             ║");
    console.log("║   Type 'exit' to quit                  ║");
    console.log("╚════════════════════════════════════════╝\n");

    while (true) {
        const query = readlineSync.question('\n💬 You: ');
        if(query.toLowerCase() === "exit") {
            console.log("👋 Goodbye!");
            break;
        }

        if(query.toLowerCase() === "help") {
            console.log(TOOL_DESCRIPTIONS);
            continue;
        }

        sessionContext.conversationCount++;
        let nextMessage = JSON.stringify({ type: 'user', user: query });
        let stepCount = 0;
        const maxSteps = 10; // Prevent infinite loops

        while (stepCount < maxSteps) {
            try {
                const result = await chat.sendMessage(nextMessage);
                const responseText = result.response.text();
                const cleanText = responseText.replace(/```json|```/g, "").trim();

                const call = JSON.parse(cleanText);
                stepCount++;

                // Display agent's thinking process
                if (call.type === "start") {
                    console.log(`\n🧠 Thinking: ${call.thought}`);
                    nextMessage = "proceed";
                } else if (call.type === "plan") {
                    console.log(`\n📋 Plan: ${call.plan}`);
                    if (call.steps) {
                        console.log(`   Steps: ${call.steps.join(' → ')}`);
                    }
                    nextMessage = "proceed";
                } else if (call.type === "action") {
                    console.log(`\n🔧 Action: ${call.function}(${call.input})`);
                    const fn = tools[call.function];
                    if (!fn) {
                        nextMessage = JSON.stringify({ 
                            "type": "observation", 
                            "observation": "Error: Tool not found" 
                        });
                    } else {
                        // Handle both sync and async functions
                        let observation = fn(call.input);
                        if (observation instanceof Promise) {
                            observation = await observation;
                        }
                        // Convert observation to string if it isn't already
                        const obsStr = typeof observation === 'string' ? observation : JSON.stringify(observation);
                        console.log(`   Result: ${obsStr.substring(0, 100)}${obsStr.length > 100 ? '...' : ''}`);
                        nextMessage = JSON.stringify({ 
                            "type": "observation", 
                            "observation": obsStr 
                        });
                    }
                } else if (call.type === "observation") {
                    nextMessage = "continue";
                } else if (call.type === "output") {
                    console.log(`\n🤖 Agent: ${call.output}\n`);
                    break;
                } else {
                    nextMessage = "proceed";
                }
            } catch (error) {
                console.error("❌ Error:", error.message);
                break;
            }
        }

        if (stepCount >= maxSteps) {
            console.log("⚠️  Agent exceeded maximum steps. Please try rephrasing your request.");
        }
    }
}

run();
