package io.github.rygel.outerstellar.world;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WorldCitiesService.
 * Tests all query methods with an in-memory H2 database.
 */
@Tag("integration")
class WorldCitiesServiceIntegrationTest {

    private DataSource dataSource;
    private WorldCitiesService service;

    @BeforeEach
    void setUp() throws Exception {
        // Create in-memory H2 database with unique name for test isolation
        String dbName = "test-world-cities-" + UUID.randomUUID().toString().substring(0, 8);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        config.setUsername("sa");
        config.setPassword("");
        dataSource = new HikariDataSource(config);

        // Create tables and insert test data
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create countries table
            stmt.execute("CREATE TABLE countries (" +
                "id BIGINT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "iso3 CHAR(3), " +
                "numeric_code CHAR(3), " +
                "iso2 CHAR(2), " +
                "phonecode VARCHAR(10), " +
                "capital VARCHAR(255), " +
                "currency VARCHAR(3), " +
                "currency_name VARCHAR(100), " +
                "currency_symbol VARCHAR(10), " +
                "tld VARCHAR(10), " +
                "native_name VARCHAR(255), " +
                "population BIGINT, " +
                "gdp BIGINT, " +
                "region VARCHAR(100), " +
                "region_id BIGINT, " +
                "subregion VARCHAR(100), " +
                "subregion_id BIGINT, " +
                "nationality VARCHAR(100), " +
                "area_sq_km DOUBLE, " +
                "postal_code_format VARCHAR(100), " +
                "postal_code_regex VARCHAR(255), " +
                "timezones VARCHAR(500), " +
                "translations CLOB, " +
                "latitude DOUBLE, " +
                "longitude DOUBLE, " +
                "emoji VARCHAR(10), " +
                "emoji_u VARCHAR(50), " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP, " +
                "flag SMALLINT, " +
                "wiki_data_id VARCHAR(50)" +
                ")");

            // Create cities table
            stmt.execute("CREATE TABLE cities (" +
                "id BIGINT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "state_id BIGINT, " +
                "state_code VARCHAR(10), " +
                "country_id BIGINT, " +
                "country_code VARCHAR(2), " +
                "type VARCHAR(50), " +
                "level INTEGER, " +
                "parent_id BIGINT, " +
                "latitude DOUBLE, " +
                "longitude DOUBLE, " +
                "native_name VARCHAR(255), " +
                "population BIGINT, " +
                "timezone VARCHAR(100), " +
                "translations CLOB, " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP, " +
                "flag SMALLINT, " +
                "wiki_data_id VARCHAR(50)" +
                ")");

            // Create states table
            stmt.execute("CREATE TABLE states (" +
                "id BIGINT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "country_id BIGINT, " +
                "country_code VARCHAR(2), " +
                "fips_code VARCHAR(10), " +
                "iso2 VARCHAR(10), " +
                "type VARCHAR(50), " +
                "latitude DOUBLE, " +
                "longitude DOUBLE, " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP, " +
                "flag SMALLINT, " +
                "wiki_data_id VARCHAR(50)" +
                ")");

            // Insert test countries
            stmt.execute("INSERT INTO countries (id, name, iso2, capital, population, region, subregion) VALUES " +
                "(1, 'Germany', 'DE', 'Berlin', 83000000, 'Europe', 'Western Europe'), " +
                "(2, 'France', 'FR', 'Paris', 67000000, 'Europe', 'Western Europe'), " +
                "(3, 'United States', 'US', 'Washington, D.C.', 331000000, 'Americas', 'North America'), " +
                "(4, 'Japan', 'JP', 'Tokyo', 125800000, 'Asia', 'Eastern Asia')");

            // Insert test cities
            stmt.execute("INSERT INTO cities (id, name, country_id, country_code, latitude, longitude, population) VALUES " +
                "(1, 'Berlin', 1, 'DE', 52.5200, 13.4050, 3644826), " +
                "(2, 'Hamburg', 1, 'DE', 53.5511, 9.9937, 1841179), " +
                "(3, 'Munich', 1, 'DE', 48.1351, 11.5820, 1471508), " +
                "(4, 'Paris', 2, 'FR', 48.8566, 2.3522, 2161000), " +
                "(5, 'Lyon', 2, 'FR', 45.7640, 4.8357, 513275), " +
                "(6, 'New York', 3, 'US', 40.7128, -74.0060, 8336817), " +
                "(7, 'Los Angeles', 3, 'US', 34.0522, -118.2437, 3980400), " +
                "(8, 'Tokyo', 4, 'JP', 35.6762, 139.6503, 13960000)");

            // Insert test states
            stmt.execute("INSERT INTO states (id, name, country_id, country_code, type) VALUES " +
                "(1, 'Bavaria', 1, 'DE', 'state'), " +
                "(2, 'California', 3, 'US', 'state'), " +
                "(3, 'New York', 3, 'US', 'state')");

            conn.commit();
        }

        service = new WorldCitiesService(dataSource);
    }

    @AfterEach
    void tearDown() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    @Test
    void shouldFindCityById() {
        Optional<WorldCitiesService.City> city = service.findCityById(1);
        
        assertThat(city).isPresent();
        assertThat(city.get().name()).isEqualTo("Berlin");
        assertThat(city.get().countryCode()).isEqualTo("DE");
        assertThat(city.get().population()).isEqualTo(3644826);
    }

    @Test
    void shouldReturnEmptyWhenCityNotFound() {
        Optional<WorldCitiesService.City> city = service.findCityById(999);
        
        assertThat(city).isEmpty();
    }

    @Test
    void shouldSearchCitiesByName() {
        List<WorldCitiesService.City> cities = service.searchCitiesByName("Berlin", 10);
        
        assertThat(cities).hasSize(1);
        assertThat(cities.get(0).name()).isEqualTo("Berlin");
    }

    @Test
    void shouldSearchCitiesByNameCaseInsensitive() {
        List<WorldCitiesService.City> cities = service.searchCitiesByName("berlin", 10);
        
        assertThat(cities).hasSize(1);
        assertThat(cities.get(0).name()).isEqualTo("Berlin");
    }

    @Test
    void shouldSearchCitiesByPartialName() {
        List<WorldCitiesService.City> cities = service.searchCitiesByName("burg", 10);
        
        assertThat(cities).hasSize(1);
        assertThat(cities).extracting(WorldCitiesService.City::name)
            .containsExactly("Hamburg");
    }

    @Test
    void shouldLimitSearchResults() {
        List<WorldCitiesService.City> cities = service.searchCitiesByName("a", 3);
        
        assertThat(cities).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void shouldFindCitiesByCountryCode() {
        List<WorldCitiesService.City> cities = service.findCitiesByCountryCode("DE", 10);
        
        assertThat(cities).hasSize(3);
        assertThat(cities).extracting(WorldCitiesService.City::name)
            .containsExactlyInAnyOrder("Berlin", "Hamburg", "Munich");
    }

    @Test
    void shouldFindCitiesByCountryCodeCaseInsensitive() {
        List<WorldCitiesService.City> cities = service.findCitiesByCountryCode("de", 10);
        
        assertThat(cities).hasSize(3);
    }

    @Test
    void shouldLimitCitiesByCountry() {
        List<WorldCitiesService.City> cities = service.findCitiesByCountryCode("US", 1);
        
        assertThat(cities).hasSize(1);
    }

    @Test
    void shouldFindCountryByCode() {
        Optional<WorldCitiesService.Country> country = service.findCountryByCode("JP");
        
        assertThat(country).isPresent();
        assertThat(country.get().name()).isEqualTo("Japan");
        assertThat(country.get().capital()).isEqualTo("Tokyo");
    }

    @Test
    void shouldFindCountryByCodeCaseInsensitive() {
        Optional<WorldCitiesService.Country> country = service.findCountryByCode("jp");
        
        assertThat(country).isPresent();
        assertThat(country.get().name()).isEqualTo("Japan");
    }

    @Test
    void shouldReturnEmptyWhenCountryNotFound() {
        Optional<WorldCitiesService.Country> country = service.findCountryByCode("XX");
        
        assertThat(country).isEmpty();
    }

    @Test
    void shouldSearchCountriesByName() {
        List<WorldCitiesService.Country> countries = service.searchCountriesByName("United", 10);
        
        assertThat(countries).hasSize(1);
        assertThat(countries.get(0).name()).isEqualTo("United States");
    }

    @Test
    void shouldSearchCountriesByNameCaseInsensitive() {
        List<WorldCitiesService.Country> countries = service.searchCountriesByName("germany", 10);
        
        assertThat(countries).hasSize(1);
        assertThat(countries.get(0).name()).isEqualTo("Germany");
    }

    @Test
    void shouldGetAllCountries() {
        List<WorldCitiesService.Country> countries = service.getAllCountries();
        
        assertThat(countries).hasSize(4);
        assertThat(countries).extracting(WorldCitiesService.Country::name)
            .containsExactly("France", "Germany", "Japan", "United States"); // Alphabetical order
    }

    @Test
    void shouldGetCityCount() {
        long count = service.getCityCount();
        
        assertThat(count).isEqualTo(8);
    }

    @Test
    void shouldGetCountryCount() {
        long count = service.getCountryCount();
        
        assertThat(count).isEqualTo(4);
    }

    @Test
    void shouldFindNearbyCities() {
        // Search within 50km of Berlin center
        List<WorldCitiesService.City> nearby = service.findNearbyCities(52.52, 13.405, 50.0, 10);
        
        assertThat(nearby).isNotEmpty();
        // Berlin should be the closest (or in the results)
        assertThat(nearby.stream().map(WorldCitiesService.City::name))
            .contains("Berlin");
    }

    @Test
    void shouldFindNearbyCitiesWithLimit() {
        List<WorldCitiesService.City> nearby = service.findNearbyCities(52.52, 13.405, 1000.0, 2);
        
        assertThat(nearby).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void shouldReturnEmptyForNoNearbyCities() {
        // Search in the middle of the ocean
        List<WorldCitiesService.City> nearby = service.findNearbyCities(0.0, 0.0, 10.0, 10);
        
        assertThat(nearby).isEmpty();
    }

    @Test
    void shouldOrderNearbyCitiesByDistance() {
        // Search near Berlin
        List<WorldCitiesService.City> nearby = service.findNearbyCities(52.52, 13.405, 1000.0, 10);
        
        if (nearby.size() >= 2) {
            // First city should be closest (Berlin)
            assertThat(nearby.get(0).name()).isEqualTo("Berlin");
        }
    }
}
