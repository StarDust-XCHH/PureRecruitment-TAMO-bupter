package com.bupt.tarecruit.common.ai;

import java.io.IOException;

public class FileParsingPendingException extends IOException {
    public FileParsingPendingException(String fileName, Throwable cause) {
        super("文件尚未完成解析: " + fileName, cause);
    }
}
