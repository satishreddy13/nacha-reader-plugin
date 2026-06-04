package com.example.pdi.plugin.nachareader.nacha;

/**
 * Configuration for NACHA structural validation.
 * All header fields default to "" which means "do not check this field".
 */
public class NachaValidationConfig implements Cloneable {

    // ---- Header validation (empty = skip) ----
    private String expectedImmediateDestination = "";
    private String expectedImmediateOrigin      = "";
    private String expectedCompanyName          = "";
    private String expectedSecCode              = "";

    // ---- Numeric integrity checks ----
    /** Verify entry/addenda counts in batch control and file control records. */
    private boolean checkCounts  = true;
    /** Verify debit/credit totals and entry hash in batch and file control records. */
    private boolean checkAmounts = true;

    // ---- Accessors ----
    public String  getExpectedImmediateDestination()           { return expectedImmediateDestination; }
    public void    setExpectedImmediateDestination(String v)   { expectedImmediateDestination = nvl(v); }
    public String  getExpectedImmediateOrigin()                { return expectedImmediateOrigin; }
    public void    setExpectedImmediateOrigin(String v)        { expectedImmediateOrigin = nvl(v); }
    public String  getExpectedCompanyName()                    { return expectedCompanyName; }
    public void    setExpectedCompanyName(String v)            { expectedCompanyName = nvl(v); }
    public String  getExpectedSecCode()                        { return expectedSecCode; }
    public void    setExpectedSecCode(String v)                { expectedSecCode = nvl(v); }
    public boolean isCheckCounts()                             { return checkCounts; }
    public void    setCheckCounts(boolean v)                   { checkCounts = v; }
    public boolean isCheckAmounts()                            { return checkAmounts; }
    public void    setCheckAmounts(boolean v)                  { checkAmounts = v; }

    @Override
    public NachaValidationConfig clone() {
        try { return (NachaValidationConfig) super.clone(); }
        catch (CloneNotSupportedException e) { throw new AssertionError(e); }
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}
