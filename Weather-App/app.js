const countrySelect = document.querySelector("#country-select");
const cityInput = document.querySelector("#city-input");
const form = document.querySelector("#weather-form");
const submitButton = document.querySelector("#submit-button");
const statusMessage = document.querySelector("#status-message");
const emptyState = document.querySelector("#empty-state");
const resultsPanel = document.querySelector("#results-panel");
const locationTitle = document.querySelector("#location-title");
const welcomeLine = document.querySelector("#welcome-line");
const conditionText = document.querySelector("#condition-text");
const updatedText = document.querySelector("#updated-text");
const tempCEl = document.querySelector("#temp-c");
const tempFEl = document.querySelector("#temp-f");
const metricsGrid = document.querySelector("#metrics-grid");
const situationSummary = document.querySelector("#situation-summary");
const advisorySummary = document.querySelector("#advisory-summary");
const seasonIntro = document.querySelector("#season-intro");
const seasonGrid = document.querySelector("#season-grid");
const precautionsGrid = document.querySelector("#precautions-grid");

const COUNTRY_MAP = new Map(COUNTRIES.map((country) => [country.code, country.name]));

const WEATHER_CODES = {
  0: "Clear sky",
  1: "Mainly clear",
  2: "Partly cloudy",
  3: "Overcast",
  45: "Fog",
  48: "Rime fog",
  51: "Light drizzle",
  53: "Moderate drizzle",
  55: "Dense drizzle",
  56: "Light freezing drizzle",
  57: "Dense freezing drizzle",
  61: "Slight rain",
  63: "Moderate rain",
  65: "Heavy rain",
  66: "Light freezing rain",
  67: "Heavy freezing rain",
  71: "Slight snowfall",
  73: "Moderate snowfall",
  75: "Heavy snowfall",
  77: "Snow grains",
  80: "Slight rain showers",
  81: "Moderate rain showers",
  82: "Violent rain showers",
  85: "Slight snow showers",
  86: "Heavy snow showers",
  95: "Thunderstorm",
  96: "Thunderstorm with slight hail",
  99: "Thunderstorm with heavy hail",
};

const INDIA_SEASONS = Object.freeze([
  {
    key: "spring",
    name: "Spring",
    range: "Mid February to April",
    start: "02-15",
    end: "04-30",
    description: "A softer stretch with lighter mornings and steadily warmer afternoons.",
    precautions: [
      "Use light layers because mornings and evenings can still shift in temperature.",
      "Carry water on longer days outside as the afternoon warmth starts to build.",
      "If pollen is an issue, keep windows closed during dusty or breezy hours.",
    ],
  },
  {
    key: "summer",
    name: "Summer",
    range: "May to June",
    start: "05-01",
    end: "06-30",
    description: "Heat builds quickly, especially through the afternoon, before the main rains arrive.",
    precautions: [
      "Drink water regularly and avoid long exposure in direct afternoon sun.",
      "Choose breathable clothing and plan outdoor activity for early morning or evening.",
      "Watch for heat stress signs such as dizziness, nausea, or unusual fatigue.",
    ],
  },
  {
    key: "monsoon",
    name: "Monsoon",
    range: "July to September",
    start: "07-01",
    end: "09-30",
    description: "Frequent rain, humidity, and changing road conditions shape daily life.",
    precautions: [
      "Keep a compact umbrella or rain layer ready for sudden showers.",
      "Avoid waterlogged areas and allow extra travel time when rain intensifies.",
      "Use dry footwear and keep electronics protected from moisture.",
    ],
  },
  {
    key: "autumn",
    name: "Autumn",
    range: "October to November",
    start: "10-01",
    end: "11-30",
    description: "Humidity eases, skies often settle, and conditions feel more balanced.",
    precautions: [
      "Keep hydration steady because warm afternoons can still catch up with you.",
      "Air quality can fluctuate in some cities, so mask up if haze becomes noticeable.",
      "Lighter evening layers help when the temperature drops after sunset.",
    ],
  },
  {
    key: "winter",
    name: "Winter",
    range: "December to Mid February",
    start: "12-01",
    end: "02-14",
    description: "Cooler mornings and nights arrive, with fog or cold spells in many regions.",
    precautions: [
      "Add a layer for early mornings and nights, especially when wind picks up.",
      "Fog can reduce visibility, so travel more slowly and keep lights visible.",
      "Dry skin and dry air are common, so use water and moisturizer more consistently.",
    ],
  },
]);

const NORTHERN_SEASONS = Object.freeze([
  {
    key: "spring",
    name: "Spring",
    range: "March to May",
    start: "03-01",
    end: "05-31",
    description: "Temperatures recover, trees bloom, and daylight lengthens.",
    precautions: [
      "Keep a light layer nearby because temperatures can swing across the day.",
      "Pollen and breezy conditions can be irritating, so plan for allergies if needed.",
      "Rain can be frequent, so a small water-resistant layer helps.",
    ],
  },
  {
    key: "summer",
    name: "Summer",
    range: "June to August",
    start: "06-01",
    end: "08-31",
    description: "Longer days and stronger sun make this the warmest part of the year.",
    precautions: [
      "Stay hydrated and use shade or sunscreen when UV exposure is high.",
      "Avoid the hottest part of the afternoon if the temperature becomes oppressive.",
      "Ventilate indoor spaces well to reduce heat buildup.",
    ],
  },
  {
    key: "autumn",
    name: "Autumn",
    range: "September to November",
    start: "09-01",
    end: "11-30",
    description: "Air cools, leaves turn, and daylight shortens week by week.",
    precautions: [
      "Keep a medium layer ready because mornings and evenings cool down quickly.",
      "Wet leaves and rain can make roads and walkways slippery.",
      "Shorter daylight means visibility matters earlier in the evening.",
    ],
  },
  {
    key: "winter",
    name: "Winter",
    range: "December to February",
    start: "12-01",
    end: "02-29",
    description: "Cold air, shorter days, and frost or snow define the season in many regions.",
    precautions: [
      "Dress in layers and protect hands, ears, and feet when the air is sharp.",
      "Watch for ice, frost, or low visibility before driving or walking.",
      "Dry indoor air can be tiring, so keep hydration and skin care steady.",
    ],
  },
]);

const SOUTHERN_SEASONS = Object.freeze([
  {
    key: "autumn",
    name: "Autumn",
    range: "March to May",
    start: "03-01",
    end: "05-31",
    description: "Heat eases, the air settles, and evenings gradually cool down.",
    precautions: [
      "Carry a layer for cooler evenings and changing winds.",
      "Rain and wet roads can still appear unexpectedly, so keep plans flexible.",
      "Use the milder weather to reset routines before colder weeks arrive.",
    ],
  },
  {
    key: "winter",
    name: "Winter",
    range: "June to August",
    start: "06-01",
    end: "08-31",
    description: "Cooler temperatures, shorter days, and regional frost or snow become more likely.",
    precautions: [
      "Layer clothing and keep warm extras close when mornings turn sharp.",
      "Check local visibility and road conditions if fog, frost, or snow are common.",
      "Indoor heating can dry the air, so keep hydration consistent.",
    ],
  },
  {
    key: "spring",
    name: "Spring",
    range: "September to November",
    start: "09-01",
    end: "11-30",
    description: "Conditions brighten, temperatures rise, and plant life becomes more active again.",
    precautions: [
      "Keep a light layer because warm afternoons may still lead into cool evenings.",
      "Allergy-sensitive days can increase with pollen and wind.",
      "Spring showers can surprise you, so a compact rain layer helps.",
    ],
  },
  {
    key: "summer",
    name: "Summer",
    range: "December to February",
    start: "12-01",
    end: "02-29",
    description: "The strongest sun and warmest temperatures arrive with longer, brighter days.",
    precautions: [
      "Use sun protection, hydration, and shade during peak daylight hours.",
      "Do not underestimate heat on overcast days because UV can still stay high.",
      "Keep indoor spaces ventilated when warm air lingers at night.",
    ],
  },
]);

populateCountries();

form.addEventListener("submit", async (event) => {
  event.preventDefault();

  const city = cityInput.value.trim();
  const countryCode = countrySelect.value;

  if (city.length < 2) {
    setStatus("Please enter at least two characters for the city.", "error");
    cityInput.focus();
    return;
  }

  setLoading(true);
  setStatus(`Looking up ${city}, ${getCountryName(countryCode)}...`);

  try {
    const location = await findLocation(city, countryCode);
    const forecast = await fetchForecast(location.latitude, location.longitude);
    renderWeather(location, forecast);
    emptyState.hidden = true;
    resultsPanel.hidden = false;
    setStatus(`Showing live weather for ${location.name}, ${getCountryName(countryCode)}.`, "success");
  } catch (error) {
    setStatus(error.message || "Unable to fetch the weather right now.", "error");
  } finally {
    setLoading(false);
  }
});

function populateCountries() {
  const optionsMarkup = COUNTRIES.map(
    (country) => `<option value="${country.code}">${escapeHtml(country.name)}</option>`
  ).join("");

  countrySelect.innerHTML = optionsMarkup;
  countrySelect.value = "IN";
  cityInput.value = "";
  setStatus("Countries loaded. Enter a city and get the latest weather.");
}

async function findLocation(city, countryCode) {
  const searchParams = new URLSearchParams({
    name: city,
    count: "10",
    language: "en",
    format: "json",
    countryCode,
  });

  const response = await fetch(`https://geocoding-api.open-meteo.com/v1/search?${searchParams}`);

  if (!response.ok) {
    throw new Error("Location search failed. Please try again in a moment.");
  }

  const data = await response.json();
  const results = Array.isArray(data.results) ? data.results : [];

  if (!results.length) {
    throw new Error(`I could not find \"${city}\" in ${getCountryName(countryCode)}.`);
  }

  return pickBestLocation(results, city, countryCode);
}

function pickBestLocation(results, city, countryCode) {
  const normalizedCity = normalize(city);

  return [...results].sort((left, right) => {
    return scoreLocation(right, normalizedCity, countryCode) - scoreLocation(left, normalizedCity, countryCode);
  })[0];
}

function scoreLocation(location, normalizedCity, countryCode) {
  let score = 0;
  const normalizedName = normalize(location.name || "");

  if (location.country_code === countryCode) {
    score += 40;
  }

  if (normalizedName === normalizedCity) {
    score += 30;
  } else if (normalizedName.startsWith(normalizedCity)) {
    score += 18;
  } else if (normalizedCity.startsWith(normalizedName)) {
    score += 12;
  }

  if (location.feature_code === "PPLC") {
    score += 10;
  }

  if (location.feature_code === "PPLA") {
    score += 7;
  }

  score += Math.min((location.population || 0) / 1000000, 10);
  return score;
}

async function fetchForecast(latitude, longitude) {
  const params = new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    current:
      "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_gusts_10m,cloud_cover",
    daily: "temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max",
    timezone: "auto",
    forecast_days: "1",
  });

  const response = await fetch(`https://api.open-meteo.com/v1/forecast?${params}`);

  if (!response.ok) {
    throw new Error("Weather service did not respond. Please try again shortly.");
  }

  return response.json();
}

function renderWeather(location, forecast) {
  const current = forecast.current;
  const daily = {
    temperatureMax: forecast.daily?.temperature_2m_max?.[0],
    temperatureMin: forecast.daily?.temperature_2m_min?.[0],
    sunrise: forecast.daily?.sunrise?.[0],
    sunset: forecast.daily?.sunset?.[0],
    uvIndexMax: forecast.daily?.uv_index_max?.[0],
  };

  const countryName = getCountryName(location.country_code);
  const displayName = [location.name, location.admin1, countryName].filter(Boolean).join(", ");
  const weatherLabel = describeWeather(current.weather_code);
  const seasonProfile = getSeasonProfile(location, current.time);
  const currentSeason = seasonProfile.seasons.find((season) => season.active) || seasonProfile.seasons[0];
  const tempF = celsiusToFahrenheit(current.temperature_2m);

  locationTitle.textContent = displayName;
  welcomeLine.textContent = buildWelcomeLine(location.name, current.weather_code, current.is_day);
  conditionText.textContent = `${weatherLabel}. Local time is ${formatDateTime(current.time)}.`;
  updatedText.textContent = `Coordinates: ${location.latitude.toFixed(2)}, ${location.longitude.toFixed(2)}.`;
  tempCEl.textContent = formatNumber(current.temperature_2m);
  tempFEl.textContent = formatNumber(tempF);

  metricsGrid.innerHTML = buildMetricsMarkup(current, daily);
  situationSummary.textContent = buildSituationSummary(location.name, current, daily, weatherLabel);
  advisorySummary.textContent = buildCurrentAdvisory(current, daily, currentSeason);
  seasonIntro.textContent = seasonProfile.intro;
  seasonGrid.innerHTML = buildSeasonMarkup(seasonProfile.seasons);
  precautionsGrid.innerHTML = buildPrecautionMarkup(seasonProfile.seasons);

  document.documentElement.dataset.theme = pickTheme(current.weather_code, current.is_day, current.temperature_2m);
}

function buildMetricsMarkup(current, daily) {
  const metrics = [
    ["Feels like", `${formatNumber(current.apparent_temperature)} deg C`],
    ["Humidity", `${Math.round(current.relative_humidity_2m)}%`],
    ["Wind", `${formatNumber(current.wind_speed_10m)} km/h`],
    ["Wind gusts", `${formatNumber(current.wind_gusts_10m)} km/h`],
    ["Precipitation", `${formatNumber(current.precipitation)} mm`],
    ["Cloud cover", `${Math.round(current.cloud_cover)}%`],
    ["Today high", `${formatNumber(daily.temperatureMax)} deg C`],
    ["Today low", `${formatNumber(daily.temperatureMin)} deg C`],
    ["UV peak", daily.uvIndexMax != null ? formatNumber(daily.uvIndexMax) : "Unavailable"],
    ["Sunrise", formatTimeOnly(daily.sunrise)],
    ["Sunset", formatTimeOnly(daily.sunset)],
    ["Daylight", computeDaylight(daily.sunrise, daily.sunset)],
  ];

  return metrics
    .map(
      ([label, value]) => `
        <div class="metric-card">
          <div class="metric-label">${escapeHtml(label)}</div>
          <div class="metric-value">${escapeHtml(value)}</div>
        </div>
      `
    )
    .join("");
}

function buildSituationSummary(cityName, current, daily, weatherLabel) {
  const tempTone = describeTemperature(current.temperature_2m);
  const windTone = describeWind(current.wind_speed_10m);
  const dayRange = `Today is moving between ${formatNumber(daily.temperatureMin)} and ${formatNumber(
    daily.temperatureMax
  )} deg C.`;

  return `${cityName} feels ${tempTone} right now with ${weatherLabel.toLowerCase()}. The air feels like ${formatNumber(
    current.apparent_temperature
  )} deg C, humidity is ${Math.round(current.relative_humidity_2m)}%, and the wind is ${windTone}. ${dayRange}`;
}

function buildCurrentAdvisory(current, daily, currentSeason) {
  const notes = [];

  if (current.temperature_2m >= 35) {
    notes.push("Heat is the main risk today, so water, shade, and lighter plans matter.");
  } else if (current.temperature_2m <= 5) {
    notes.push("Cold exposure can build quietly, so add layers before you need them.");
  }

  if (current.precipitation > 0.2 || [61, 63, 65, 80, 81, 82].includes(current.weather_code)) {
    notes.push("Keep a rain layer nearby and allow extra travel time on wet roads.");
  }

  if (daily.uvIndexMax >= 6) {
    notes.push("UV is elevated today, so sun protection is worth keeping close.");
  }

  if (current.wind_gusts_10m >= 35) {
    notes.push("Gusty conditions can make the air feel harsher than the temperature suggests.");
  }

  if (!notes.length) {
    notes.push(`This is a good ${currentSeason.name.toLowerCase()} day for simple preparation rather than major adjustments.`);
  }

  return notes.join(" ");
}

function getSeasonProfile(location, isoTime) {
  const monthDay = (isoTime || "").slice(5, 10);

  if (location.country_code === "IN") {
    return {
      intro:
        "For India, the dashboard uses a monsoon-aware seasonal calendar because it is usually more practical than a generic four-season split.",
      seasons: INDIA_SEASONS.map((season) => ({
        ...season,
        active: isMonthDayWithinRange(monthDay, season.start, season.end),
      })),
    };
  }

  const baseSeasons = location.latitude < 0 ? SOUTHERN_SEASONS : NORTHERN_SEASONS;
  const intro =
    location.latitude < 0
      ? "This location uses the Southern Hemisphere meteorological seasons, so summer and winter are reversed from India."
      : "This location uses the Northern Hemisphere meteorological seasons, which follow the standard March, June, September, and December transitions.";

  return {
    intro,
    seasons: baseSeasons.map((season) => ({
      ...season,
      active: isMonthDayWithinRange(monthDay, season.start, season.end),
    })),
  };
}

function buildSeasonMarkup(seasons) {
  return seasons
    .map(
      (season) => `
        <article class="season-card${season.active ? " active" : ""}">
          <div class="season-header">
            <div>
              <h3>${escapeHtml(season.name)}</h3>
              <p class="season-range">${escapeHtml(season.range)}</p>
            </div>
            ${season.active ? '<span class="season-chip">Current</span>' : ""}
          </div>
          <p class="season-description">${escapeHtml(season.description)}</p>
        </article>
      `
    )
    .join("");
}

function buildPrecautionMarkup(seasons) {
  return seasons
    .map(
      (season) => `
        <article class="precaution-card">
          <div class="season-header">
            <div>
              <h3>${escapeHtml(season.name)}</h3>
              <p class="season-range">${escapeHtml(season.range)}</p>
            </div>
            ${season.active ? '<span class="season-chip">Focus now</span>' : ""}
          </div>
          <ul class="precaution-list">
            ${season.precautions.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}
          </ul>
        </article>
      `
    )
    .join("");
}

function buildWelcomeLine(cityName, weatherCode, isDay) {
  const label = describeWeather(weatherCode).toLowerCase();

  if (!isDay) {
    return `Hello there. ${cityName} has a quieter evening feel right now with ${label}.`;
  }

  if ([61, 63, 65, 80, 81, 82].includes(weatherCode)) {
    return `Hello there. ${cityName} is moving through a wetter stretch right now, so here is the practical view.`;
  }

  return `Hello there. Here is a calm, live read on the weather in ${cityName}.`;
}

function pickTheme(weatherCode, isDay, temperatureC) {
  if (!isDay) {
    return "night";
  }

  if ([95, 96, 99].includes(weatherCode)) {
    return "storm";
  }

  if ([71, 73, 75, 77, 85, 86].includes(weatherCode)) {
    return "snow";
  }

  if ([45, 48].includes(weatherCode)) {
    return "mist";
  }

  if ([51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82].includes(weatherCode)) {
    return "rain";
  }

  if (weatherCode === 0 && temperatureC >= 30) {
    return "warm";
  }

  if ([1, 2].includes(weatherCode)) {
    return temperatureC >= 30 ? "warm" : "clear-day";
  }

  return "cloudy";
}

function describeWeather(code) {
  return WEATHER_CODES[code] || "Current conditions";
}

function describeTemperature(value) {
  if (value >= 35) {
    return "very hot";
  }

  if (value >= 28) {
    return "warm";
  }

  if (value >= 18) {
    return "comfortable";
  }

  if (value >= 10) {
    return "cool";
  }

  return "cold";
}

function describeWind(value) {
  if (value >= 35) {
    return "strong";
  }

  if (value >= 20) {
    return "noticeable";
  }

  if (value >= 8) {
    return "gentle";
  }

  return "light";
}

function isMonthDayWithinRange(monthDay, start, end) {
  if (!monthDay) {
    return false;
  }

  if (start <= end) {
    return monthDay >= start && monthDay <= end;
  }

  return monthDay >= start || monthDay <= end;
}

function computeDaylight(sunrise, sunset) {
  if (!sunrise || !sunset) {
    return "Unavailable";
  }

  const sunriseDate = new Date(sunrise);
  const sunsetDate = new Date(sunset);
  const minutes = Math.round((sunsetDate - sunriseDate) / 60000);

  if (!Number.isFinite(minutes) || minutes <= 0) {
    return "Unavailable";
  }

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return `${hours}h ${remainingMinutes}m`;
}

function formatDateTime(isoString) {
  if (!isoString) {
    return "unavailable";
  }

  const [datePart, timePart] = isoString.split("T");

  if (!datePart || !timePart) {
    return isoString;
  }

  const [year, month, day] = datePart.split("-").map(Number);
  const [hour, minute] = timePart.split(":").map(Number);
  const monthName = new Intl.DateTimeFormat("en", { month: "long" }).format(new Date(year, month - 1, day));

  return `${monthName} ${day}, ${year} at ${formatHourMinute(hour, minute)}`;
}

function formatTimeOnly(isoString) {
  if (!isoString) {
    return "Unavailable";
  }

  const [, timePart] = isoString.split("T");

  if (!timePart) {
    return isoString;
  }

  const [hour, minute] = timePart.split(":").map(Number);
  return formatHourMinute(hour, minute);
}

function formatHourMinute(hour, minute) {
  const period = hour >= 12 ? "PM" : "AM";
  const normalizedHour = hour % 12 || 12;
  return `${normalizedHour}:${String(minute).padStart(2, "0")} ${period}`;
}

function setLoading(isLoading) {
  submitButton.disabled = isLoading;
  submitButton.textContent = isLoading ? "Fetching..." : "Get weather";
}

function setStatus(message, type = "") {
  statusMessage.textContent = message;
  statusMessage.className = `status-message${type ? ` ${type}` : ""}`;
}

function getCountryName(countryCode) {
  return COUNTRY_MAP.get(countryCode) || countryCode;
}

function celsiusToFahrenheit(celsius) {
  return (celsius * 9) / 5 + 32;
}

function formatNumber(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return "--";
  }

  return Number(value).toFixed(1);
}

function normalize(value) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}


