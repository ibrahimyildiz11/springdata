package be.vdab.springdata.repositories;

import be.vdab.springdata.domain.Filiaal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest(showSql = false)
@Sql("/insertFilialen.sql")
class FiliaalRepositoryTest  extends AbstractTransactionalJUnit4SpringContextTests {
    private static final String FILIALEN = "filialen";
    private final FiliaalRepository repository;

    FiliaalRepositoryTest(FiliaalRepository repository) {
        this.repository = repository;
    }

    private long idVanAlfa() {
        return jdbcTemplate.queryForObject(
                "select id from filialen where naam = 'Alfa'", Long.class);
    }

    private long idVanBravo() {
        return jdbcTemplate.queryForObject(
                "select id from filialen where naam = 'Bravo'", Long.class);
    }

    @Test
    void count() {
        assertThat(repository.count()).isEqualTo(countRowsInTable(FILIALEN));
    }

    @Test
    void findById() {
        assertThat(repository.findById(idVanAlfa()))
                .hasValueSatisfying(
                        filiaal -> assertThat(filiaal.getNaam()).isEqualTo("Alfa"));
    }

    @Test
    void findAll() {
        assertThat(repository.findAll())
                .hasSize(super.countRowsInTable(FILIALEN));
    }
    @Test
    void findAllGesorteerdOpGemeente() {
        assertThat(repository.findAll(Sort.by("gemeente")))
                .hasSize(super.countRowsInTable(FILIALEN))
                .extracting(Filiaal::getGemeente)
                .isSortedAccordingTo(String::compareToIgnoreCase);
    }
    @Test
    void findAllById() {
        var idAlfa = idVanAlfa();
        var idBravo = idVanBravo();
        assertThat(repository.findAllById(Set.of(idVanAlfa(), idVanBravo())))
                .extracting(Filiaal::getId)
                .containsOnly(idAlfa,idBravo);
    }
    @Test
    void save() {
        var filiaal = new Filiaal("Delta", "Brugge", BigDecimal.TEN);
        repository.save(filiaal);
        var id = filiaal.getId();
        assertThat(id).isPositive();
        assertThat(countRowsInTableWhere(FILIALEN, "id = " + id)).isOne();
    }

    @Test
    void deleteById() {
        var id = idVanAlfa();
        repository.deleteById(id);
        repository.flush();
        assertThat(countRowsInTableWhere(FILIALEN, "id = " + id)).isZero();
    }

    @Test
    void deleteByOnbestaandeId() {
        assertThatExceptionOfType(EmptyResultDataAccessException.class).isThrownBy(
                () -> repository.deleteById(-1L));
    }

    @Test
    void findByGemeenteOrderByNaam() {
        assertThat(repository.findByGemeenteOrderByNaam("Brussel"))
                .hasSize(countRowsInTableWhere(FILIALEN, "gemeente = 'Brussel'"))
                .allSatisfy(filiaal ->
                        assertThat(filiaal.getGemeente()).isEqualToIgnoringCase("Brussel"))
                .extracting(Filiaal::getNaam)
                .isSortedAccordingTo(String::compareToIgnoreCase);
    }

    @Test
    void findByOmzetGreaterThanEqual() {
        var tweeduizend = BigDecimal.valueOf(2_000);
        assertThat(repository.findByOmzetGreaterThanEqual(tweeduizend))
                .hasSize(countRowsInTableWhere(FILIALEN, "omzet >= 2000"))
                .allSatisfy(filiaal ->
                        assertThat(filiaal.getOmzet()).isGreaterThanOrEqualTo(tweeduizend));
    }

    @Test void countByGemeente() {
        assertThat(repository.countByGemeente("Brussel"))
            .isEqualTo(countRowsInTableWhere(FILIALEN, "gemeente = 'Brussel'")); }

    @Test
    void findGemiddeldeOmzet() {
        assertThat(repository.findGemiddeldeOmzet())
                .isEqualByComparingTo(jdbcTemplate.queryForObject(
                        "select avg(omzet) from filialen", BigDecimal.class));
    }

    @Test
    void findMetHoogsteOmzet() {
        assertThat(repository.findMetHoogsteOmzet())
                .hasSize(countRowsInTableWhere(FILIALEN,
                        "omzet = (select max(omzet) from filialen)"))
                .first().extracting(Filiaal::getNaam).isEqualTo(
                        jdbcTemplate.queryForObject(
                                "select naam from filialen where omzet = (select max(omzet) from filialen)",
                                String.class));
    }
}