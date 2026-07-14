package com.sil.asset_tagging_system.exception;

public class UserException extends AssetTaggingSystemException{
    // if no specific user related exception is catched but its realted to user and userservice then this exception catches it
    public UserException() {
    }

    public UserException(String message) {
        super(message);
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }
}
