package io.github.rygel.outerstellar.world;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Service for querying world cities data from the H2 database.
 * Provides methods to search for cities, countries, and geographic coordinates.
 */
public class WorldCitiesService {

    private final Jdbi jdbi;

    /**
     * Creates a new service with the specified DataSource.
     *
     * @param dataSource the DataSource connected to the world cities H2 database
     */
    public WorldCitiesService(DataSource dataSource) {
        this.jdbi = Jdbi.create(dataSource);
        this.jdbi.installPlugin(new SqlObjectPlugin());
        this.jdbi.registerRowMapper(ConstructorMapper.factory(City.class));
        this.jdbi.registerRowMapper(ConstructorMapper.factory(Country.class));
        this.jdbi.registerRowMapper(ConstructorMapper.factory(State.class));
    }

    /**
     * Creates a new service with the specified JDBC URL.
     *
     * @param jdbcUrl JDBC URL for the H2 database
     */
    public WorldCitiesService(String jdbcUrl) {
        this.jdbi = Jdbi.create(jdbcUrl);
        this.jdbi.installPlugin(new SqlObjectPlugin());
        this.jdbi.registerRowMapper(ConstructorMapper.factory(City.class));
        this.jdbi.registerRowMapper(ConstructorMapper.factory(Country.class));
        this.jdbi.registerRowMapper(ConstructorMapper.factory(State.class));
    }

    /**
     * Finds a city by its ID.
     *
     * @param id the city ID
     * @return Optional containing the city if found
     */
    public Optional<City> findCityById(long id) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM cities WHERE id = :id")
                .bind("id", id)
                .mapTo(City.class)
                .findOne()
        );
    }

    /**
     * Searches for cities by name (case-insensitive partial match).
     *
     * @param name the city name to search for
     * @param limit maximum number of results
     * @return list of matching cities
     */
    public List<City> searchCitiesByName(String name, int limit) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM cities "
                    + "WHERE LOWER(name) LIKE LOWER(:pattern) "
                    + "ORDER BY population DESC NULLS LAST "
                    + "LIMIT :limit")
                .bind("pattern", "%" + name + "%")
                .bind("limit", limit)
                .mapTo(City.class)
                .list()
        );
    }

    /**
     * Finds cities by country code.
     *
     * @param countryCode the 2-letter country code (e.g., "US", "DE")
     * @param limit maximum number of results
     * @return list of cities in the country
     */
    public List<City> findCitiesByCountryCode(String countryCode, int limit) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM cities "
                    + "WHERE country_code = :code "
                    + "ORDER BY population DESC NULLS LAST "
                    + "LIMIT :limit")
                .bind("code", countryCode.toUpperCase())
                .bind("limit", limit)
                .mapTo(City.class)
                .list()
        );
    }

    /**
     * Finds a city by name and country code.
     * Returns the most populous city if multiple matches exist.
     *
     * @param cityName the city name (case-insensitive)
     * @param countryCode the 2-letter country code (e.g., "US", "DE")
     * @return Optional containing the city if found
     */
    public Optional<City> findCityByNameAndCountry(String cityName, String countryCode) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM cities "
                    + "WHERE LOWER(name) = LOWER(:name) "
                    + "AND country_code = :code "
                    + "ORDER BY population DESC NULLS LAST "
                    + "LIMIT 1")
                .bind("name", cityName)
                .bind("code", countryCode.toUpperCase())
                .mapTo(City.class)
                .findOne()
        );
    }

    /**
     * Finds a country by its ISO code.
     *
     * @param isoCode the 2-letter ISO code (e.g., "US", "DE")
     * @return Optional containing the country if found
     */
    public Optional<Country> findCountryByCode(String isoCode) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM countries WHERE iso2 = :code")
                .bind("code", isoCode.toUpperCase())
                .mapTo(Country.class)
                .findOne()
        );
    }

    /**
     * Searches for countries by name.
     *
     * @param name the country name to search for
     * @param limit maximum number of results
     * @return list of matching countries
     */
    public List<Country> searchCountriesByName(String name, int limit) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM countries "
                    + "WHERE LOWER(name) LIKE LOWER(:pattern) "
                    + "ORDER BY name "
                    + "LIMIT :limit")
                .bind("pattern", "%" + name + "%")
                .bind("limit", limit)
                .mapTo(Country.class)
                .list()
        );
    }

    /**
     * Finds a country by exact name match (case-insensitive).
     *
     * @param name the country name
     * @return Optional containing the country if found
     */
    public Optional<Country> findCountryByName(String name) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM countries "
                    + "WHERE LOWER(name) = LOWER(:name) "
                    + "LIMIT 1")
                .bind("name", name)
                .mapTo(Country.class)
                .findOne()
        );
    }

    /**
     * Finds all countries.
     *
     * @return list of all countries
     */
    public List<Country> getAllCountries() {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM countries ORDER BY name")
                .mapTo(Country.class)
                .list()
        );
    }

    /**
     * Finds cities near a specific coordinate.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @param radiusKm the search radius in kilometers
     * @param limit maximum number of results
     * @return list of nearby cities
     */
    public List<City> findNearbyCities(double latitude, double longitude,
                                       double radiusKm, int limit) {
        // Using the Haversine formula approximation
        double latDelta = radiusKm / 111.0; // Approximate degrees per km
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT *, "
                    + "(6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) "
                    + "* cos(radians(longitude) - radians(:lon)) "
                    + "+ sin(radians(:lat)) * sin(radians(latitude)))) AS distance "
                    + "FROM cities "
                    + "WHERE latitude BETWEEN :minLat AND :maxLat "
                    + "AND longitude BETWEEN :minLon AND :maxLon "
                    + "ORDER BY distance "
                    + "LIMIT :limit")
                .bind("lat", latitude)
                .bind("lon", longitude)
                .bind("minLat", latitude - latDelta)
                .bind("maxLat", latitude + latDelta)
                .bind("minLon", longitude - lonDelta)
                .bind("maxLon", longitude + lonDelta)
                .bind("limit", limit)
                .mapTo(City.class)
                .list()
        );
    }

    /**
     * Gets the total count of cities in the database.
     *
     * @return the number of cities
     */
    public long getCityCount() {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT COUNT(*) FROM cities")
                .mapTo(Long.class)
                .one()
        );
    }

    /**
     * Gets the total count of countries in the database.
     *
     * @return the number of countries
     */
    public long getCountryCount() {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT COUNT(*) FROM countries")
                .mapTo(Long.class)
                .one()
        );
    }

    /**
     * Record representing a City.
     */
    public record City(
        long id,
        String name,
        long stateId,
        String stateCode,
        long countryId,
        String countryCode,
        String type,
        Integer level,
        Long parentId,
        Double latitude,
        Double longitude,
        String nativeName,
        Long population,
        String timezone,
        String translations,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Short flag,
        String wikiDataId
    ) { }

    /**
     * Record representing a Country.
     */
    public record Country(
        long id,
        String name,
        String iso3,
        String numericCode,
        String iso2,
        String phonecode,
        String capital,
        String currency,
        String currencyName,
        String currencySymbol,
        String tld,
        String nativeName,
        Long population,
        Long gdp,
        String region,
        Long regionId,
        String subregion,
        Long subregionId,
        String nationality,
        Double areaSqKm,
        String postalCodeFormat,
        String postalCodeRegex,
        String timezones,
        String translations,
        Double latitude,
        Double longitude,
        String emoji,
        String emojiU,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Short flag,
        String wikiDataId
    ) { }

    /**
     * Record representing a State/Province.
     */
    public record State(
        long id,
        String name,
        long countryId,
        String countryCode,
        String fipsCode,
        String iso2,
        String type,
        Double latitude,
        Double longitude,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Short flag,
        String wikiDataId
    ) { }
}
