package com.sil.asset_tagging_system.exception;


public class BusinessRuleException
        extends RuntimeException {


    public BusinessRuleException(
            String message
    ) {

        super(message);
    }

}