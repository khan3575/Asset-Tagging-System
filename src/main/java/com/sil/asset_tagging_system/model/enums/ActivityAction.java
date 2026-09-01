package com.sil.asset_tagging_system.model.enums;

public enum ActivityAction {

    // authentication
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    LOGOUT,
    ACCESS_DENIED,
    PASSWORD_CHANGED,

    // assets
    ASSET_REGISTERED,
    ASSET_UPDATED,
    ASSET_CONDITION_CHANGED,
    ASSET_DOCUMENT_UPLOADED,

    // custody
    CUSTODY_ASSIGNED,
    CUSTODY_TRANSFERRED,
    CUSTODY_RELEASED,

    // approvals
    REQUEST_SUBMITTED,
    REQUEST_APPROVED,
    REQUEST_REJECTED,
    REQUEST_CANCELLED,

    // users and departments
    USER_CREATED,
    USER_UPDATED,
    USER_DISABLED,
    USER_ENABLED,
    DEPARTMENT_CREATED,
    DEPARTMENT_CLOSED
}
