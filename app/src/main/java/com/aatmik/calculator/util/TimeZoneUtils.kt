// Simple TimeZoneUtils.kt with proper country names and search
package com.aatmik.calculator.util

object TimeZoneUtils {

    // Simple data class for country and timezone
    data class CountryTimeZone(
        val countryName: String,
        val cityName: String,
        val timeZoneId: String
    )

    // Complete list with proper country names and major cities
    fun getAllCountryTimeZones(): List<CountryTimeZone> {
        return listOf(
            // Major Countries with Multiple Time Zones
            CountryTimeZone("United States", "New York", "America/New_York"),
            CountryTimeZone("United States", "Los Angeles", "America/Los_Angeles"),
            CountryTimeZone("United States", "Chicago", "America/Chicago"),
            CountryTimeZone("United States", "Denver", "America/Denver"),
            CountryTimeZone("United States", "Phoenix", "America/Phoenix"),
            CountryTimeZone("United States", "Anchorage", "America/Anchorage"),
            CountryTimeZone("United States", "Honolulu", "Pacific/Honolulu"),

            CountryTimeZone("Canada", "Toronto", "America/Toronto"),
            CountryTimeZone("Canada", "Vancouver", "America/Vancouver"),
            CountryTimeZone("Canada", "Montreal", "America/Toronto"),
            CountryTimeZone("Canada", "Calgary", "America/Edmonton"),
            CountryTimeZone("Canada", "Edmonton", "America/Edmonton"),
            CountryTimeZone("Canada", "Winnipeg", "America/Winnipeg"),
            CountryTimeZone("Canada", "Halifax", "America/Halifax"),

            CountryTimeZone("Russia", "Moscow", "Europe/Moscow"),
            CountryTimeZone("Russia", "St Petersburg", "Europe/Moscow"),
            CountryTimeZone("Russia", "Novosibirsk", "Asia/Novosibirsk"),
            CountryTimeZone("Russia", "Yekaterinburg", "Asia/Yekaterinburg"),
            CountryTimeZone("Russia", "Krasnoyarsk", "Asia/Krasnoyarsk"),
            CountryTimeZone("Russia", "Irkutsk", "Asia/Irkutsk"),
            CountryTimeZone("Russia", "Vladivostok", "Asia/Vladivostok"),
            CountryTimeZone("Russia", "Magadan", "Asia/Magadan"),
            CountryTimeZone("Russia", "Kamchatka", "Asia/Kamchatka"),

            CountryTimeZone("Australia", "Sydney", "Australia/Sydney"),
            CountryTimeZone("Australia", "Melbourne", "Australia/Melbourne"),
            CountryTimeZone("Australia", "Brisbane", "Australia/Brisbane"),
            CountryTimeZone("Australia", "Perth", "Australia/Perth"),
            CountryTimeZone("Australia", "Adelaide", "Australia/Adelaide"),
            CountryTimeZone("Australia", "Darwin", "Australia/Darwin"),
            CountryTimeZone("Australia", "Hobart", "Australia/Hobart"),

            CountryTimeZone("Brazil", "São Paulo", "America/Sao_Paulo"),
            CountryTimeZone("Brazil", "Rio de Janeiro", "America/Sao_Paulo"),
            CountryTimeZone("Brazil", "Brasília", "America/Sao_Paulo"),
            CountryTimeZone("Brazil", "Salvador", "America/Bahia"),
            CountryTimeZone("Brazil", "Fortaleza", "America/Fortaleza"),
            CountryTimeZone("Brazil", "Manaus", "America/Manaus"),
            CountryTimeZone("Brazil", "Recife", "America/Recife"),

            CountryTimeZone("Mexico", "Mexico City", "America/Mexico_City"),
            CountryTimeZone("Mexico", "Tijuana", "America/Tijuana"),
            CountryTimeZone("Mexico", "Monterrey", "America/Monterrey"),
            CountryTimeZone("Mexico", "Cancun", "America/Cancun"),
            CountryTimeZone("Mexico", "Chihuahua", "America/Chihuahua"),
            CountryTimeZone("Mexico", "Mazatlan", "America/Mazatlan"),

            CountryTimeZone("India", "New Delhi", "Asia/Kolkata"),
            CountryTimeZone("India", "Mumbai", "Asia/Kolkata"),
            CountryTimeZone("India", "Bangalore", "Asia/Kolkata"),
            CountryTimeZone("India", "Kolkata", "Asia/Kolkata"),
            CountryTimeZone("India", "Chennai", "Asia/Kolkata"),
            CountryTimeZone("India", "Hyderabad", "Asia/Kolkata"),

            CountryTimeZone("China", "Beijing", "Asia/Shanghai"),
            CountryTimeZone("China", "Shanghai", "Asia/Shanghai"),
            CountryTimeZone("China", "Guangzhou", "Asia/Shanghai"),
            CountryTimeZone("China", "Shenzhen", "Asia/Shanghai"),
            CountryTimeZone("China", "Urumqi", "Asia/Urumqi"),

            // Single Time Zone Countries (Major ones)
            CountryTimeZone("United Kingdom", "London", "Europe/London"),
            CountryTimeZone("France", "Paris", "Europe/Paris"),
            CountryTimeZone("Germany", "Berlin", "Europe/Berlin"),
            CountryTimeZone("Italy", "Rome", "Europe/Rome"),
            CountryTimeZone("Spain", "Madrid", "Europe/Madrid"),
            CountryTimeZone("Netherlands", "Amsterdam", "Europe/Amsterdam"),
            CountryTimeZone("Switzerland", "Zurich", "Europe/Zurich"),
            CountryTimeZone("Austria", "Vienna", "Europe/Vienna"),
            CountryTimeZone("Belgium", "Brussels", "Europe/Brussels"),
            CountryTimeZone("Sweden", "Stockholm", "Europe/Stockholm"),
            CountryTimeZone("Norway", "Oslo", "Europe/Oslo"),
            CountryTimeZone("Denmark", "Copenhagen", "Europe/Copenhagen"),
            CountryTimeZone("Finland", "Helsinki", "Europe/Helsinki"),
            CountryTimeZone("Poland", "Warsaw", "Europe/Warsaw"),
            CountryTimeZone("Czech Republic", "Prague", "Europe/Prague"),
            CountryTimeZone("Hungary", "Budapest", "Europe/Budapest"),
            CountryTimeZone("Romania", "Bucharest", "Europe/Bucharest"),
            CountryTimeZone("Greece", "Athens", "Europe/Athens"),
            CountryTimeZone("Portugal", "Lisbon", "Europe/Lisbon"),
            CountryTimeZone("Ireland", "Dublin", "Europe/Dublin"),

            CountryTimeZone("Japan", "Tokyo", "Asia/Tokyo"),
            CountryTimeZone("South Korea", "Seoul", "Asia/Seoul"),
            CountryTimeZone("Thailand", "Bangkok", "Asia/Bangkok"),
            CountryTimeZone("Singapore", "Singapore", "Asia/Singapore"),
            CountryTimeZone("Malaysia", "Kuala Lumpur", "Asia/Kuala_Lumpur"),
            CountryTimeZone("Indonesia", "Jakarta", "Asia/Jakarta"),
            CountryTimeZone("Philippines", "Manila", "Asia/Manila"),
            CountryTimeZone("Vietnam", "Ho Chi Minh City", "Asia/Ho_Chi_Minh"),
            CountryTimeZone("Taiwan", "Taipei", "Asia/Taipei"),
            CountryTimeZone("Hong Kong", "Hong Kong", "Asia/Hong_Kong"),

            CountryTimeZone("UAE", "Dubai", "Asia/Dubai"),
            CountryTimeZone("Saudi Arabia", "Riyadh", "Asia/Riyadh"),
            CountryTimeZone("Israel", "Jerusalem", "Asia/Jerusalem"),
            CountryTimeZone("Turkey", "Istanbul", "Europe/Istanbul"),
            CountryTimeZone("Iran", "Tehran", "Asia/Tehran"),
            CountryTimeZone("Pakistan", "Karachi", "Asia/Karachi"),
            CountryTimeZone("Afghanistan", "Kabul", "Asia/Kabul"),
            CountryTimeZone("Bangladesh", "Dhaka", "Asia/Dhaka"),
            CountryTimeZone("Sri Lanka", "Colombo", "Asia/Colombo"),
            CountryTimeZone("Nepal", "Kathmandu", "Asia/Kathmandu"),

            CountryTimeZone("Egypt", "Cairo", "Africa/Cairo"),
            CountryTimeZone("South Africa", "Johannesburg", "Africa/Johannesburg"),
            CountryTimeZone("Nigeria", "Lagos", "Africa/Lagos"),
            CountryTimeZone("Kenya", "Nairobi", "Africa/Nairobi"),
            CountryTimeZone("Morocco", "Casablanca", "Africa/Casablanca"),
            CountryTimeZone("Algeria", "Algiers", "Africa/Algiers"),
            CountryTimeZone("Tunisia", "Tunis", "Africa/Tunis"),
            CountryTimeZone("Libya", "Tripoli", "Africa/Tripoli"),
            CountryTimeZone("Ghana", "Accra", "Africa/Accra"),
            CountryTimeZone("Ethiopia", "Addis Ababa", "Africa/Addis_Ababa"),

            CountryTimeZone("Argentina", "Buenos Aires", "America/Argentina/Buenos_Aires"),
            CountryTimeZone("Chile", "Santiago", "America/Santiago"),
            CountryTimeZone("Peru", "Lima", "America/Lima"),
            CountryTimeZone("Colombia", "Bogotá", "America/Bogota"),
            CountryTimeZone("Venezuela", "Caracas", "America/Caracas"),
            CountryTimeZone("Ecuador", "Quito", "America/Guayaquil"),
            CountryTimeZone("Bolivia", "La Paz", "America/La_Paz"),
            CountryTimeZone("Paraguay", "Asunción", "America/Asuncion"),
            CountryTimeZone("Uruguay", "Montevideo", "America/Montevideo"),

            CountryTimeZone("New Zealand", "Auckland", "Pacific/Auckland"),
            CountryTimeZone("Fiji", "Suva", "Pacific/Fiji"),
            CountryTimeZone("Iceland", "Reykjavik", "Atlantic/Reykjavik"),
            CountryTimeZone("Greenland", "Nuuk", "America/Nuuk")
        ).sortedBy { "${it.countryName} - ${it.cityName}" }
    }

    // Simple search function
    fun searchTimeZones(query: String): List<CountryTimeZone> {
        if (query.isBlank()) return getAllCountryTimeZones()

        val searchTerm = query.lowercase().trim()
        return getAllCountryTimeZones().filter { timeZone ->
            timeZone.countryName.lowercase().contains(searchTerm) ||
                    timeZone.cityName.lowercase().contains(searchTerm)
        }
    }

    // Helper function to get display name
    fun getDisplayName(timeZone: CountryTimeZone): String {
        return "${timeZone.countryName} - ${timeZone.cityName}"
    }

    // Legacy functions for compatibility
    fun getCityName(timeZoneId: String): String {
        val timeZone = getAllCountryTimeZones().find { it.timeZoneId == timeZoneId }
        return timeZone?.cityName ?: timeZoneId.split("/").lastOrNull()?.replace("_", " ") ?: timeZoneId
    }

    fun getCountryName(timeZoneId: String): String {
        val timeZone = getAllCountryTimeZones().find { it.timeZoneId == timeZoneId }
        return timeZone?.countryName ?: "Unknown"
    }
}