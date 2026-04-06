import { GoogleGenerativeAI } from "@google/generative-ai";

// ---------------- CONFIGURATION ----------------
const GEMINI_API_KEY = ""; // PASTE YOU API_KEY(I HAVE USED GEMINI)
// No Weather API Key needed for Open-Meteo!
// -----------------------------------------------

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);

// 1. Define the Open-Meteo Tool
async function get_weather_details(city) {
    try {
        console.log(`[System] Fetching coordinates for: ${city}...`);
        
        // Step A: Geocoding (Convert City Name -> Lat/Long)
        // Open-Meteo needs Lat/Long, so we use their free geocoding API first.
        const geoUrl = `https://geocoding-api.open-meteo.com/v1/search?name=${city}&count=1&language=en&format=json`;
        const geoResponse = await fetch(geoUrl);
        const geoData = await geoResponse.json();

        if (!geoData.results || geoData.results.length === 0) {
            return `Error: Could not find coordinates for city: ${city}`;
        }

        const { latitude, longitude, name, country } = geoData.results[0];

        // Step B: Get Weather using Lat/Long
        console.log(`[System] Fetching weather for ${name}, ${country} (${latitude}, ${longitude})...`);
        const weatherUrl = `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto`;
        
        const weatherResponse = await fetch(weatherUrl);
        const weatherData = await weatherResponse.json();

        // Step C: Format Data
        const current = weatherData.current;
        const weatherInfo = {
            location: `${name}, ${country}`,
            temperature: `${current.temperature_2m}${weatherData.current_units.temperature_2m}`,
            humidity: `${current.relative_humidity_2m}${weatherData.current_units.relative_humidity_2m}`,
            wind_speed: `${current.wind_speed_10m}${weatherData.current_units.wind_speed_10m}`,
            // Open-Meteo returns a numeric code (WMO code) for description
            description: interpretWeatherCode(current.weather_code) 
        };

        return JSON.stringify(weatherInfo);

    } catch (error) {
        return `Error fetching weather data: ${error.message}`;
    }
}

// Helper to translate WMO codes to text
function interpretWeatherCode(code) {
    const codes = {
        0: "Clear sky", 1: "Mainly clear", 2: "Partly cloudy", 3: "Overcast",
        45: "Fog", 48: "Depositing rime fog",
        51: "Light drizzle", 53: "Moderate drizzle", 55: "Dense drizzle",
        61: "Slight rain", 63: "Moderate rain", 65: "Heavy rain",
        80: "Slight rain showers", 81: "Moderate rain showers", 82: "Violent rain showers",
        95: "Thunderstorm"
    };
    return codes[code] || "Unknown weather code";
}

// 2. Tool Definition
const tools = [
  {
    functionDeclarations: [
      {
        name: "get_weather_details",
        description: "Get the current weather for a specific city.",
        parameters: {
          type: "OBJECT",
          properties: {
            city: {
              type: "STRING",
              description: "The name of the city to get weather for.",
            },
          },
          required: ["city"],
        },
      },
    ],
  },
];

async function run() {
  try {
    const model = genAI.getGenerativeModel({ 
        model: "gemini-2.5-flash", 
        tools: tools 
    });

    const chat = model.startChat();
    const userQuery = "What is the weather like in New Delhi right now?";

    console.log(`User: ${userQuery}`);

    const result = await chat.sendMessage(userQuery);
    const response = result.response;
    const calls = response.functionCalls();
    const call = calls && calls.length > 0 ? calls[0] : null;

    if (call) {
        const apiResponse = await get_weather_details(call.args.city);
        console.log(`[System] API returned: ${apiResponse}`);

        const result2 = await chat.sendMessage([
          {
            functionResponse: {
              name: "get_weather_details",
              response: { name: "get_weather_details", content: apiResponse },
            },
          },
        ]);

        console.log(`Gemini: ${result2.response.text()}`);
    } else {
        console.log(`Gemini: ${response.text()}`);
    }
  } catch (error) {
    console.error("Error:", error);
  }
}

run();
