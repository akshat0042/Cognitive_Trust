package org.learn.securecodeproject.model;

public class ScanFinding {
    public String check_id;
    public String path;
    public int start_line;
    public int end_line;
    public String message;
    public String extra; // optional marker like "auto_fix_replace_secret"
    public String suggestion; // human-friendly suggestion
}