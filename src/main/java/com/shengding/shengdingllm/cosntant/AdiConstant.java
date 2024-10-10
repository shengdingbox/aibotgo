package com.shengding.shengdingllm.cosntant;

public class AdiConstant {

    public static final String PROMPT_TEMPLATE = """
            根据以下已知信息:
            {{information}}
            尽可能准确地回答用户的问题,以下是用户的问题:
            {{question}}
            注意,回答的内容不能让用户感知到已知信息的存在
            """;

    public static final String[] POI_DOC_TYPES = {"doc", "docx", "ppt", "pptx", "xls", "xlsx"};

    public static final String CHAT_IO = "chatId";


    public static class ModelPlatform {
        public static final String QIHOO360 = "QIHOO360";
        public static final String ChatGLM3 = "ChatGLM3";
        public static final String CHATGLM = "CHATGLM";
        public static final String KIMI = "KIMI";
        public static final String XUNFEI = "XUNFEI";
        public static final String SKYWORK = "SKYWORK";
        public static final String CLAUDE = "CLAUDE";
        public static String MISTRAL = "MISTRAL";
        public static String MOSS = "MOSS";
        public static String PI = "PI";
        public static String TONGYI = "TONGYI";
        public static String YOUCHAT = "YOUCHAT";
        public static String PERPLEXITY = "PERPLEXITY";
        public static String MINIMAX = "MINIMAX";
        public static String DEEPSEEK_CHAT = "DEEPSEEK_CHAT";
    }

    public static class ModelType {
        public static final String TEXT = "text";
        public static final String IMAGE = "image";
        public static final String EMBEDDING = "embedding";
        public static final String RERANK = "rerank";
    }

    public static class SearchEngineName {
        public static final String GOOGLE = "google";
        public static final String BING = "bing";
        public static final String BAIDU = "baidu";
    }

    public static class SSEEventName {
        public static final String START = "[START]";
        public static final String DONE = "[DONE]";
        public static final String ERROR = "[ERROR]";

        public static final String AI_SEARCH_SOURCE_LINKS = "[SOURCE_LINKS]";
    }


}
