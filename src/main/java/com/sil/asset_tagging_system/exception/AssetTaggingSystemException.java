package com.sil.asset_tagging_system.exception;

public class AssetTaggingSystemException extends RuntimeException {

    // root exception for this project if any sepecific excption of heirarchy doesnt works this one catches it and takes the user back to login page

    public AssetTaggingSystemException() {
    }

    public AssetTaggingSystemException(String message) {
        super(message);
    }

    public AssetTaggingSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
