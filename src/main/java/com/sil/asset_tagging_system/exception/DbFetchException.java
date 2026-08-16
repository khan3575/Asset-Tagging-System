package com.sil.asset_tagging_system.exception;

public class DbFetchException extends AssetTaggingSystemException{
    public DbFetchException() {
    }

    public DbFetchException(String message) {
        super(message);
    }

    public DbFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
