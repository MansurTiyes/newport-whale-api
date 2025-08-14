package com.mansurtiyes.newportwhaleapi.ingest.resolve;

// static class only to be used for norm method
public final class TextNormalizer {
    private TextNormalizer() {}

    public static String norm(String s) {
        if (s == null) return "";
        String t = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC);
        t = t.replace('\u00A0', ' ')   // NBSP -> space
                .replace('\u2013', '-')   // en dash
                .replace('\u2014', '-')   // em dash
                .replace('\u2212', '-')   // minus sign
                .replace('\u00AD', ' ');  // soft hyphen -> space
        t = t.replace('’','\'');       // curly -> straight apostrophe
        return t.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
