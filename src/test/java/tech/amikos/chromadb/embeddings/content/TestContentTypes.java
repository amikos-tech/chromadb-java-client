package tech.amikos.chromadb.embeddings.content;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestContentTypes {

    @Test
    public void testContentTextFactory() {
        Content c = Content.text("hello");
        assertEquals(1, c.getParts().size());
        assertEquals(Modality.TEXT, c.getParts().get(0).getModality());
        assertEquals("hello", c.getParts().get(0).getText());
        assertNull(c.getIntent());
    }

    @Test
    public void testContentBuilderMultipleParts() {
        Content c = Content.builder()
                .part(Part.text("hi"))
                .part(Part.image(BinarySource.fromUrl("http://img")))
                .intent(Intent.RETRIEVAL_DOCUMENT)
                .build();
        assertEquals(2, c.getParts().size());
        assertEquals(Intent.RETRIEVAL_DOCUMENT, c.getIntent());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContentPartsUnmodifiable() {
        Content c = Content.text("a");
        c.getParts().add(Part.text("b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContentBuilderEmptyPartsThrows() {
        Content.builder().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContentTextNullThrows() {
        Content.text(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContentBuilderNullPartThrows() {
        Content.builder().part(null);
    }

    @Test
    public void testPartTextFactory() {
        Part p = Part.text("t");
        assertEquals(Modality.TEXT, p.getModality());
        assertEquals("t", p.getText());
        assertNull(p.getSource());
    }

    @Test
    public void testPartImageFactory() {
        Part p = Part.image(BinarySource.fromUrl("u"));
        assertEquals(Modality.IMAGE, p.getModality());
        assertNotNull(p.getSource());
        assertNull(p.getText());
    }

    @Test
    public void testPartAudioFactory() {
        Part p = Part.audio(BinarySource.fromUrl("u"));
        assertEquals(Modality.AUDIO, p.getModality());
        assertNotNull(p.getSource());
    }

    @Test
    public void testPartVideoFactory() {
        Part p = Part.video(BinarySource.fromUrl("u"));
        assertEquals(Modality.VIDEO, p.getModality());
        assertNotNull(p.getSource());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPartTextNullThrows() {
        Part.text(null);
    }

    @Test
    public void testBinarySourceFromUrl() {
        BinarySource s = BinarySource.fromUrl("http://x");
        assertEquals("http://x", s.getUrl());
        assertNull(s.getFilePath());
        assertNull(s.getBase64Data());
        assertNull(s.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBinarySourceFromUrlNullThrows() {
        BinarySource.fromUrl(null);
    }

    @Test
    public void testBinarySourceFromFile() {
        BinarySource s = BinarySource.fromFile("/tmp/img.png");
        assertEquals("/tmp/img.png", s.getFilePath());
        assertNull(s.getUrl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBinarySourceFromFileNullThrows() {
        BinarySource.fromFile(null);
    }

    @Test
    public void testBinarySourceFromBase64() {
        BinarySource s = BinarySource.fromBase64("AQID");
        assertEquals("AQID", s.getBase64Data());
    }

    @Test
    public void testBinarySourceFromBytesDefensiveCopy() {
        byte[] original = new byte[]{1, 2, 3};
        BinarySource s = BinarySource.fromBytes(original);
        byte[] first = s.getBytes();
        assertArrayEquals(new byte[]{1, 2, 3}, first);

        // Mutate returned array
        first[0] = 99;
        // Verify source is unchanged
        assertArrayEquals(new byte[]{1, 2, 3}, s.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBinarySourceFromBytesNullThrows() {
        BinarySource.fromBytes(null);
    }

    @Test
    public void testModalityFromValue() {
        assertEquals(Modality.TEXT, Modality.fromValue("text"));
        assertEquals(Modality.IMAGE, Modality.fromValue("image"));
        assertEquals(Modality.AUDIO, Modality.fromValue("audio"));
        assertEquals(Modality.VIDEO, Modality.fromValue("video"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testModalityFromValueInvalid() {
        Modality.fromValue("bogus");
    }

    @Test
    public void testIntentFromValue() {
        assertEquals(Intent.RETRIEVAL_DOCUMENT, Intent.fromValue("retrieval_document"));
        assertEquals(Intent.RETRIEVAL_QUERY, Intent.fromValue("retrieval_query"));
        assertEquals(Intent.CLASSIFICATION, Intent.fromValue("classification"));
        assertEquals(Intent.CLUSTERING, Intent.fromValue("clustering"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIntentFromValueInvalid() {
        Intent.fromValue("bogus");
    }

    @Test
    public void testContentEquality() {
        Content a = Content.text("same");
        Content b = Content.text("same");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testContentInequality() {
        Content a = Content.text("hello");
        Content b = Content.text("world");
        assertNotEquals(a, b);
    }
}
