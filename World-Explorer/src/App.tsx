

import React, { useState, useEffect, useMemo } from 'react';
import { Search, Globe, MapPin, Users, Hash } from 'lucide-react';

interface Country {
  name: {
    common: string;
    official: string;
  };
  cca2: string;
  cca3: string;
  region: string;
  capital?: string[];
  population: number;
  flags: {
    svg: string;
    png: string;
  };
}

type SearchCriteria = 'name' | 'code' | 'continent' | 'capital';

export default function App() {
  const [countries, setCountries] = useState<Country[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [searchQuery, setSearchQuery] = useState('');
  const [searchCriteria, setSearchCriteria] = useState<SearchCriteria>('name');

  useEffect(() => {
    const fetchCountries = async () => {
      try {
        setLoading(true);
        const response = await fetch('https://restcountries.com/v3.1/all?fields=name,cca2,cca3,region,capital,population,flags');
        if (!response.ok) {
          throw new Error('Failed to fetch countries');
        }
        const data = await response.json();
        setCountries(data);
        setError(null);
      } catch (err) {
        setError('Failed to load countries. Please try again later.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchCountries();
  }, []);

  const filteredCountries = useMemo(() => {
    if (!searchQuery.trim()) return countries;

    const query = searchQuery.toLowerCase();

    return countries.filter((country) => {
      switch (searchCriteria) {
        case 'name':
          return (
            country.name.common.toLowerCase().includes(query) ||
            country.name.official.toLowerCase().includes(query)
          );
        case 'code':
          return (
            country.cca2.toLowerCase().includes(query) ||
            country.cca3.toLowerCase().includes(query)
          );
        case 'continent':
          return country.region.toLowerCase().includes(query);
        case 'capital':
          return country.capital?.some((cap) => cap.toLowerCase().includes(query));
        default:
          return true;
      }
    });
  }, [countries, searchQuery, searchCriteria]);

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 font-sans">
      <header className="bg-white shadow-sm sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex flex-col sm:flex-row justify-between items-center gap-4">
          <h1 className="text-2xl font-bold flex items-center gap-2 text-indigo-600">
            <Globe className="w-8 h-8" />
            World Explorer
          </h1>
          
          <div className="flex flex-col sm:flex-row w-full sm:w-auto gap-2">
            <div className="relative flex-grow sm:max-w-xs">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Search className="h-5 w-5 text-gray-400" />
              </div>
              <input
                type="text"
                className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                placeholder={`Search by ${searchCriteria}...`}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <select
              className="block w-full sm:w-auto pl-3 pr-10 py-2 text-base border border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md bg-white"
              value={searchCriteria}
              onChange={(e) => setSearchCriteria(e.target.value as SearchCriteria)}
            >
              <option value="name">Name</option>
              <option value="code">Country Code</option>
              <option value="continent">Continent</option>
              <option value="capital">Capital</option>
            </select>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {loading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
          </div>
        ) : error ? (
          <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded-md">
            <div className="flex">
              <div className="flex-shrink-0">
                <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <p className="text-sm text-red-700">{error}</p>
              </div>
            </div>
          </div>
        ) : filteredCountries.length === 0 ? (
          <div className="text-center py-12">
            <Globe className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">No countries found</h3>
            <p className="mt-1 text-sm text-gray-500">
              We couldn't find any countries matching your search criteria.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredCountries.map((country) => (
              <div key={country.cca3} className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow duration-200 overflow-hidden border border-gray-100 flex flex-col">
                <div className="h-40 w-full overflow-hidden bg-gray-100">
                  <img 
                    src={country.flags.svg} 
                    alt={`Flag of ${country.name.common}`} 
                    className="w-full h-full object-cover"
                    loading="lazy"
                    referrerPolicy="no-referrer"
                  />
                </div>
                <div className="p-5 flex-grow flex flex-col">
                  <h2 className="text-xl font-bold text-gray-900 mb-1 line-clamp-1" title={country.name.common}>
                    {country.name.common}
                  </h2>
                  <p className="text-sm text-gray-500 mb-4 line-clamp-1" title={country.name.official}>
                    {country.name.official}
                  </p>
                  
                  <div className="mt-auto space-y-2">
                    <div className="flex items-center text-sm text-gray-600">
                      <Users className="w-4 h-4 mr-2 text-gray-400" />
                      <span className="font-medium mr-1">Population:</span> 
                      {country.population.toLocaleString()}
                    </div>
                    <div className="flex items-center text-sm text-gray-600">
                      <Globe className="w-4 h-4 mr-2 text-gray-400" />
                      <span className="font-medium mr-1">Continent:</span> 
                      {country.region}
                    </div>
                    <div className="flex items-center text-sm text-gray-600">
                      <MapPin className="w-4 h-4 mr-2 text-gray-400" />
                      <span className="font-medium mr-1">Capital:</span> 
                      {country.capital ? country.capital.join(', ') : 'N/A'}
                    </div>
                    <div className="flex items-center text-sm text-gray-600">
                      <Hash className="w-4 h-4 mr-2 text-gray-400" />
                      <span className="font-medium mr-1">Code:</span> 
                      {country.cca2} / {country.cca3}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
