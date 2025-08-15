package com.mansurtiyes.newportwhaleapi.ingest.resolve;

import com.mansurtiyes.newportwhaleapi.ingest.WhaleCountParser;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class WhaleCountParserUnitTests {

    @Mock
    private InMemorySpeciesResolver speciesResolver;

    @InjectMocks
    private WhaleCountParser parser;

    @Test
    void parsesDateSingleDigitMonthDay() {
        LocalDate date = parser.parseDate("8/11/2025");
        assertThat(date).isEqualTo(LocalDate.of(2025, 8, 11));
    }

    @Test
    void parsesDateZeroPaddedMonthDay() {
        LocalDate date = parser.parseDate("08/01/2025");
        assertThat(date).isEqualTo(LocalDate.of(2025, 8, 1));
    }

    @Test
    void DateTrimsWhitespace() {
        LocalDate date = parser.parseDate("  7/9/2025  ");
        assertThat(date).isEqualTo(LocalDate.of(2025, 7, 9));
    }

    @Test
    void DateNullInputThrows() {
        assertThatThrownBy(() -> parser.parseDate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void DateBlankInputThrows() {
        assertThatThrownBy(() -> parser.parseDate("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void DateBadFormatThrows() {
        assertThatThrownBy(() -> parser.parseDate("2025-08-11"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ToursParsesValidZero() {
        Integer tours = parser.parseTours("0");
        assertThat(tours).isEqualTo(0);
    }

    @Test
    void ToursParsesValidTwoDigits() {
        Integer tours = parser.parseTours("14");
        assertThat(tours).isEqualTo(14);
    }

    @Test
    void ToursParsesValidWithSpaces() {
        Integer tours = parser.parseTours("   26  ");
        assertThat(tours).isEqualTo(26);
    }

    @Test
    void ToursNullInputThrows() {
        assertThatThrownBy(() -> parser.parseTours(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void ToursBlankInputThrows() {
        assertThatThrownBy(() -> parser.parseTours("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void ToursNonNumericThrows() {
        assertThatThrownBy(() -> parser.parseTours("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to parse");
    }

    @Test
    void statusReturnsBadWeatherWhenExactMatch() {
        assertThat(parser.parseStatus("Bad Weather"))
                .isEqualTo(ReportStatus.bad_weather);
    }

    @Test
    void statusReturnsBadWeatherWhenCaseDiffers() {
        assertThat(parser.parseStatus("bad weather"))
                .isEqualTo(ReportStatus.bad_weather);

        assertThat(parser.parseStatus("BAD WEATHER"))
                .isEqualTo(ReportStatus.bad_weather);

        assertThat(parser.parseStatus("BaD WeAtHeR"))
                .isEqualTo(ReportStatus.bad_weather);
    }

    @Test
    void statusReturnsBadWeatherWhenTrimmed() {
        assertThat(parser.parseStatus("  Bad Weather  "))
                .isEqualTo(ReportStatus.bad_weather);
    }

    @Test
    void statusReturnsOkForAnyOtherDescription() {
        assertThat(parser.parseStatus("850 Common Dolphin, 38 Bottlenose Dolphin"))
                .isEqualTo(ReportStatus.ok);

        assertThat(parser.parseStatus("10 Mola Mola"))
                .isEqualTo(ReportStatus.ok);

        assertThat(parser.parseStatus(""))
                .isEqualTo(ReportStatus.ok);
    }

    @Test
    void statusReturnsOkWhenDescriptionIsNull() {
        assertThat(parser.parseStatus(null))
                .isEqualTo(ReportStatus.ok);
    }

}
