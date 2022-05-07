package be.vdab.springdata.repositories;

import be.vdab.springdata.domain.Filiaal;
import be.vdab.springdata.domain.Werknemer;
import be.vdab.springdata.projections.AantalWerknemersPerFamilienaam;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest(showSql = false)
@Sql({"/insertFilialen.sql", "/insertWerknemers.sql"})
class WerknemerRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    private static final String WERKNEMERS = "werknemers";
    private final WerknemerRepository repository;

    WerknemerRepositoryTest(WerknemerRepository repository) {
        this.repository = repository;
    }

    @Test
    void findByFiliaalGemeente() {
        var antwerpen = "Antwerpen";
        assertThat(repository.findByFiliaalGemeente(antwerpen))
                .hasSize(countRowsInTableWhere(WERKNEMERS,
                        "filiaalId = (select id from filialen where gemeente = 'antwerpen')"))
                .first()
                .extracting(Werknemer::getFiliaal)
                .extracting(Filiaal::getGemeente)
                .isEqualTo(antwerpen);
    }

    @Test
    void findByVoornaamStartingWith() {
        assertThat(repository.findByVoornaamStartingWith("J"))
                .hasSize(countRowsInTableWhere(WERKNEMERS, "voornaam like 'J%'"))
                .allSatisfy(werknemer ->
                        assertThat(werknemer.getVoornaam().toUpperCase()).startsWith("J"))
                .extracting(Werknemer::getFiliaal)
                .extracting(Filiaal::getNaam);
    }

    @Test
    void eerstePagina() {
        var page = repository.findAll(PageRequest.of(0,2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isTrue();
    }
    @Test
    void tweedePagina() {
        var page = repository.findAll(PageRequest.of(1,2));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void findAantalWerknemersPerFamilienaam() {
        assertThat(repository.findAantalWerknemersPerFamilienaam())
                .hasSize(jdbcTemplate.queryForObject(
                        "select count(distinct familienaam) from werknemers", Integer.class))
                .filteredOn(aantalPerFamilienaam ->
                        aantalPerFamilienaam.getFamilienaam().equals("Dalton"))
                .hasSize(1) .first()
                .extracting(AantalWerknemersPerFamilienaam::getAantal)
                .isEqualTo(super.countRowsInTableWhere(
                        "werknemers", "familienaam = 'Dalton'"));
    }
}