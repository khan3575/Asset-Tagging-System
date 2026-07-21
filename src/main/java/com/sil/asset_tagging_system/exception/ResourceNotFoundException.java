package com.sil.asset_tagging_system.exception;

public class ResourceNotFoundException extends AssetTaggingSystemException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(
            String resourceName,
            String fieldName,
            Object fieldValue
    ) {

        super(
                "%s with %s '%s' was not found."
                        .formatted(
                                resourceName,
                                fieldName,
                                fieldValue
                        )
        );
    }

}