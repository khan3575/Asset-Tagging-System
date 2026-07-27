package com.sil.asset_tagging_system.exception;

public class DuplicateResourceException
        extends AssetTaggingSystemException {

    public DuplicateResourceException(
            String message
    ) {

        super(message);
    }

    public DuplicateResourceException(
            String resourceName,
            String fieldName,
            Object fieldValue
    ) {

        super(
                "%s with %s '%s' already exists."
                        .formatted(
                                resourceName,
                                fieldName,
                                fieldValue
                        )
        );
    }

}