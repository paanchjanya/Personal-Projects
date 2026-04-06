import { GoogleGenerativeAI } from "@google/generative-ai";
import readline from "readline";

// ---------------- CONFIGURATION ----------------
// OPTION A: Use .env file (Recommended)
// import 'dotenv/config'; 
// const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

// OPTION B: Paste key directly (Quick & Dirty)
const GEMINI_API_KEY = "API_KEY"; 
// -----------------------------------------------

// Check if key is present
if (!GEMINI_API_KEY || GEMINI_API_KEY === " ") {
    console.error("Error: Please provide a valid API Key in index2.js");
    process.exit(1);
}

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

// Setup the interface to read from the Terminal
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

async function getDefinition(word) {
    try {
        console.log(`\n[System] Asking Gemini for the meaning of "${word}"...\n`);

        // Create a focused prompt
        const prompt = `Define the word "${word}" simply and clearly. Provide: 
        1. The definition.
        2. An example sentence. 
        3. Synonyms (if any).`;

        const result = await model.generateContent(prompt);
        const response = await result.response;
        const text = response.text();

        console.log("------------------------------------------------");
        console.log(text);
        console.log("------------------------------------------------");

    } catch (error) {
        console.error("Error connecting to Gemini:", error.message);
    }
}

// Start the interaction
rl.question("Enter a word to define: ", async (userInput) => {
    if (!userInput.trim()) {
        console.log("You didn't enter a word!");
    } else {
        await getDefinition(userInput);
    }
    
    // Close the program after finishing
    rl.close();
});
