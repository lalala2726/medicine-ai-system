package com.zhangyichuang.medicine.common.systemauth.core;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemAuthCanonicalBuilderTests {

    private final SystemAuthCanonicalBuilder builder = new SystemAuthCanonicalBuilder();

    @Test
    void buildSortedQuery_ShouldSortAndKeepDuplicateParams() {
        String rawQuery = "b=2&a=2&a=1&space=hello+world&special=%E4%B8%AD%E6%96%87";

        String result = builder.buildSortedQuery(rawQuery);

        assertEquals("a=1&a=2&b=2&space=hello%20world&special=%E4%B8%AD%E6%96%87", result);
    }

    @Test
    void buildSortedQuery_ByHttpUrl_ShouldSortQuery() {
        HttpUrl url = HttpUrl.parse("https://example.com/test?z=2&a=2&a=1");

        String result = builder.buildSortedQuery(url);

        assertEquals("a=1&a=2&z=2", result);
    }

    @Test
    void sha256Hex_WhenEmptyBody_ShouldReturnExpectedHash() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                builder.sha256Hex(new byte[0])
        );
    }
}
