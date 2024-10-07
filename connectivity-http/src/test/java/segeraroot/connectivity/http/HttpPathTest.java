package segeraroot.connectivity.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpPathTest {

    @Test
    void name() {
        HttpPath parsed = HttpPath.parse("foo/bar/baz");

        assertThat(parsed.nameCount()).isEqualTo(3);
        assertThat(parsed.name(0)).isEqualTo("foo");
        assertThat(parsed.name(1)).isEqualTo("bar");
        assertThat(parsed.name(2)).isEqualTo("baz");
    }

    @Test
    void rest() {
        HttpPath parsed = HttpPath.parse("foo/bar/baz")
                .rest(0);
        assertThat(parsed.nameCount()).isEqualTo(2);
        assertThat(parsed.name(0)).isEqualTo("bar");
        assertThat(parsed.name(1)).isEqualTo("baz");
    }
}
