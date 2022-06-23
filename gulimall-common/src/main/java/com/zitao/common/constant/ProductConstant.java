package com.zitao.common.constant;

import org.bouncycastle.cms.PasswordRecipientId;

public class ProductConstant {

    public enum AttrEnum {
        ATTR_TYPE_BASE(1, "base property"), ATTR_TYPE_SALE(0, "sale property");

        private int code;
        private String msg;

        AttrEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
