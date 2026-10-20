package com.hostelsync.backend.dto;

/**
 * Represents an uploaded file in a complaint response.
 * Matches frontend EvidenceFile: { id, name, size, type, url }
 */
public class AttachmentDto {

    private String id;
    private String name;    // original file name shown to user
    private long size;
    private String type;    // MIME type
    private String url;     // download endpoint URL

    public AttachmentDto() {}

    public AttachmentDto(String id, String name, long size, String type, String url) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.type = type;
        this.url = url;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
