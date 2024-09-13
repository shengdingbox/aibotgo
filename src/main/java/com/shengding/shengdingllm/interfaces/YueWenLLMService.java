package com.shengding.shengdingllm.interfaces;

import com.alibaba.fastjson.JSONObject;
import com.shengding.shengdingllm.vo.AssistantChatParams;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class YueWenLLMService extends AbstractLLMService{

//    public YueWenLLMService() {
//        MODE_NAME = "step";
//    }


//// 模型名称
//
//// access_token有效期
//const ACCESS_TOKEN_EXPIRES = 900;
//// 最大重试次数
//const MAX_RETRY_COUNT = 0;
//// 重试延迟
//const RETRY_DELAY = 5000;


    private Map<String, String> getAuthHeader() {
        Map<String, String> pairs = new HashMap<>();
        pairs.put("Accept", "*/*");
        pairs.put("Accept-Language", "zh-CN,zh;q=0.9");
        pairs.put("Origin", "https://yuewen.cn");
        pairs.put("Connect-Protocol-Version", "1");
        pairs.put("Oasis-Appid", "10200");
        pairs.put("Oasis-Platform", "web");
        pairs.put("Sec-Ch-Ua", "Chromium;v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"");
        pairs.put("Sec-Ch-Ua-Mobile", "?0");
        pairs.put("Sec-Ch-Ua-Platform", "Windows");
        pairs.put("Sec-Fetch-Dest", "empty");
        pairs.put("Sec-Fetch-Mode", "cors");
        pairs.put("Sec-Fetch-Site", "same-origin");
        pairs.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        return pairs;
    }


    /**
     * 请求access_token
     * <p>
     * 使用refresh_token去刷新获得access_token
     *
     * @param refreshToken 用于刷新access_token的refresh_token
     */
    // 请求access_token的方法
    public void requestToken(String refreshToken) {
        // 如果已经在队列中，则直接加入队列等待结果
        // 记录日志
        System.out.println("刷新token: " + refreshToken);
        // 创建请求
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .build();
        String deviceId = refreshToken.split("@")[0];
        String token = refreshToken.split("@")[1];

        Headers.Builder builder = new Headers.Builder();
        getAuthHeader().forEach(builder::add);
        Headers headers = builder
                .add("Cookie", "Oasis-Token=" + token)
                .add("Referer", "https://yuewen.cn/chats/new")
                .add("Oasis-Webid", deviceId)
                // 添加其他伪造的头部信息
                .build();

        RequestBody body = RequestBody.create("{}", MediaType.parse("application/json")); // 空请求体
        Request request = new Request.Builder()
                .url("https://yuewen.cn/passport/proto.api.passport.v1.PassportService/RegisterDevice")
                .post(body)
                .headers(headers)
                .build();

        // 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            String responseBody = response.body().string();
            byte[] bytes = responseBody.getBytes("GBK");
            String decodedBody = new String(bytes, StandardCharsets.UTF_8);
            // 假设解析成功，获取到accessToken和refreshToken
            JSONObject jsonObject = JSONObject.parseObject(decodedBody);
            JSONObject accessTokenRaw = jsonObject.getJSONObject("accessToken");
            String accessToken = accessTokenRaw.getString("raw");
            JSONObject refreshTokenRaw = jsonObject.getJSONObject("refreshToken");
            refreshToken = refreshTokenRaw.getString("raw");
            long refreshTime = System.currentTimeMillis() + accessTokenRaw.getInteger("duration"); // 假设token有效期为1小时
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        YueWenLLMService yueWenLLMService = new YueWenLLMService();
        yueWenLLMService.requestToken("fcd05478c979787b8bab09e02658f315da7e25e2@eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3RpdmF0ZWQiOnRydWUsImFnZSI6MSwiYmFuZWQiOmZhbHNlLCJjcmVhdGVfYXQiOjE3MjYyMDkxMjIsImV4cCI6MTcyNjIxMDkyMiwibW9kZSI6Miwib2FzaXNfaWQiOjE0NTc3NTY0MjYzMjQ2MjMzNiwidmVyc2lvbiI6Mn0.DP1apLM3IUaifpLdW6n8uzKNZUKRSxhrdjylXtEuU-w...eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfaWQiOjEwMjAwLCJkZXZpY2VfaWQiOiJmY2QwNTQ3OGM5Nzk3ODdiOGJhYjA5ZTAyNjU4ZjMxNWRhN2UyNWUyIiwiZXhwIjoxNzI4ODAxMTIyLCJvYXNpc19pZCI6MTQ1Nzc1NjQyNjMyNDYyMzM2LCJwbGF0Zm9ybSI6IndlYiIsInZlcnNpb24iOjJ9.OZ6NE7US1Ri_Kruey0Buk_9w-UZLevLJUWqtW9QD3yE");
    }


    /**
     * 流式对话补全
     *
     * @param model 模型名称
     * @param messages 参考gpt系列消息格式，多轮对话请完整提供上下文
     * @param refreshToken 用于刷新access_token的refresh_token
     * @param useSearch 是否开启联网搜索
     * @param retryCount 重试次数
     */
    @Override
    protected void sendPrompt(AssistantChatParams assistantChatParams, BiConsumer<Object, Map<String, Object>> onUpdateResponse, Object callbackParam) {


//            return (async () => {
//                    logger.info(messages);
//
//            // 提取引用文件URL并上传step获得引用的文件ID列表
//    const refFileUrls = extractRefFileUrls(messages);
//    const refs = refFileUrls.length
//                    ? await Promise.all(
//                    refFileUrls.map((fileUrl) => uploadFile(fileUrl, refreshToken))
//        )
//      : [];

            // 创建会话
        createChatContext()
    const convId = await cr("新会话", refreshToken);

            // 请求流
    const { deviceId, token } = await acquireToken(refreshToken);
    const result = await axios.post(
                    `https://yuewen.cn/api/proto.chat.v1.ChatMessageService/SendMessageStream`,
            messagesPrepare(convId, messages, refs),
                    {
                            headers: {
                "Content-Type": "application/connect+json",
                        Cookie: generateCookie(deviceId, token),
                        "Oasis-Webid": deviceId,
                        Referer: `https://yuewen.cn/chats/${convId}`,
          ...FAKE_HEADERS,
            },
            // 120秒超时
            timeout: 120000,
                    validateStatus: () => true,
                    responseType: "stream",
      }
    );

    const streamStartTime = util.timestamp();
            // 创建转换流将消息格式转换为gpt兼容格式
            return createTransStream(model, convId, result.data, () => {
                    logger.success(
                            `Stream has completed transfer ${util.timestamp() - streamStartTime}ms`
      );
            // 流传输结束后异步移除会话，如果消息不合规，此操作可能会抛出数据库错误异常，请忽略
            removeConversation(convId, refreshToken).catch((err) =>
                    console.error(err)
      );
    });
  })().catch((err) => {
            if (retryCount < MAX_RETRY_COUNT) {
                logger.error(`Stream response error: ${err.message}`);
                logger.warn(`Try again after ${RETRY_DELAY / 1000}s...`);
                return (async () => {
                        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY));
                return createCompletionStream(
                        model,
                        messages,
                        refreshToken,
                        useSearch,
                        retryCount + 1
                );
      })();
            }
            throw err;
  });
        }
    }

    @Override
    protected JSONObject createChatContext(String chatId) {
        return null;
    }

    @Override
    protected boolean checkAvailability() {
        return false;
    }
}


//
//        /**
//         * 创建会话
//         *
//         * 创建临时的会话用于对话补全
//         *
//         * @param refreshToken 用于刷新access_token的refresh_token
//         */
//        async function createConversation(name:string,refreshToken:string){
//        const{deviceId,token}=await acquireToken(refreshToken);
//        const result=await axios.post(
//        "https://yuewen.cn/api/proto.chat.v1.ChatService/CreateChat",
//        {
//        chatName:name,
//        },
//        {
//        headers:{
//        Cookie:generateCookie(deviceId,token),
//        "Oasis-Webid":deviceId,
//        Referer:"https://yuewen.cn/chats/new",
//        ...FAKE_HEADERS,
//        },
//        timeout:15000,
//        validateStatus:()=>true,
//        }
//        );
//        const{chatId:convId}=checkResult(result,refreshToken);
//        return convId;
//        }
//
//        /**
//         * 移除会话
//         *
//         * 在对话流传输完毕后移除会话，避免创建的会话出现在用户的对话列表中
//         *
//         * @param refreshToken 用于刷新access_token的refresh_token
//         */
//        async function removeConversation(convId:string,refreshToken:string){
//        const{deviceId,token}=await acquireToken(refreshToken);
//        const result=await axios.post(
//        `https://yuewen.cn/api/proto.chat.v1.ChatService/DelChat`,
//        {
//        chatIds:[convId],
//        },
//        {
//        headers:{
//        Cookie:generateCookie(deviceId,token),
//        "Oasis-Webid":deviceId,
//        Referer: `https://yuewen.cn/chats/${convId}`,
//        ...FAKE_HEADERS,
//        },
//        timeout:15000,
//        validateStatus:()=>true,
//        }
//        );
//        checkResult(result,refreshToken);
//        }
//
//        /**
//         * 同步对话补全
//         *
//         * @param model 模型名称
//         * @param messages 参考gpt系列消息格式，多轮对话请完整提供上下文
//         * @param refreshToken 用于刷新access_token的refresh_token
//         * @param useSearch 是否开启联网搜索
//         * @param retryCount 重试次数
//         */
//        async function createCompletion(
//        model=MODEL_NAME,
//        messages:any[],
//        refreshToken:string,
//        useSearch=true,
//        retryCount=0
//        ){
//        return(async()=>{
//        logger.info(messages);
//
//        // 提取引用文件URL并上传step获得引用的文件ID列表
//        const refFileUrls=extractRefFileUrls(messages);
//        const refs=refFileUrls.length
//        ?await Promise.all(
//        refFileUrls.map((fileUrl)=>uploadFile(fileUrl,refreshToken))
//        )
//        :[];
//
//        // 创建会话
//        const convId=await createConversation("新会话",refreshToken);
//
//        // 请求流
//        const{deviceId,token}=await acquireToken(refreshToken);
//        const result=await axios.post(
//        `https://yuewen.cn/api/proto.chat.v1.ChatMessageService/SendMessageStream`,
//        messagesPrepare(convId,messages,refs),
//        {
//        headers:{
//        "Content-Type":"application/connect+json",
//        Cookie:generateCookie(deviceId,token),
//        "Oasis-Webid":deviceId,
//        Referer: `https://yuewen.cn/chats/${convId}`,
//        ...FAKE_HEADERS,
//        },
//        // 120秒超时
//        timeout:120000,
//        validateStatus:()=>true,
//        responseType:"stream",
//        }
//        );
//
//        const streamStartTime=util.timestamp();
//        // 接收流为输出文本
//        const answer=await receiveStream(model,convId,result.data);
//        logger.success(
//        `Stream has completed transfer ${util.timestamp()-streamStartTime}ms`
//        );
//
//        // 异步移除会话，如果消息不合规，此操作可能会抛出数据库错误异常，请忽略
//        removeConversation(convId,refreshToken).catch((err)=>console.error(err));
//
//        return answer;
//        })().catch((err)=>{
//        if(retryCount<MAX_RETRY_COUNT){
//        logger.error(`Stream response error:${err.message}`);
//        logger.warn(`Try again after ${RETRY_DELAY/1000}s...`);
//        return(async()=>{
//        await new Promise((resolve)=>setTimeout(resolve,RETRY_DELAY));
//        return createCompletion(
//        model,
//        messages,
//        refreshToken,
//        useSearch,
//        retryCount+1
//        );
//        })();
//        }
//        throw err;
//        });
//        }
//
//        /**
//         * 流式对话补全
//         *
//         * @param model 模型名称
//         * @param messages 参考gpt系列消息格式，多轮对话请完整提供上下文
//         * @param refreshToken 用于刷新access_token的refresh_token
//         * @param useSearch 是否开启联网搜索
//         * @param retryCount 重试次数
//         */
//        async function createCompletionStream(
//        model=MODEL_NAME,
//        messages:any[],
//        refreshToken:string,
//        useSearch=true,
//        retryCount=0
//        ){
//        return(async()=>{
//        logger.info(messages);
//
//        // 提取引用文件URL并上传step获得引用的文件ID列表
//        const refFileUrls=extractRefFileUrls(messages);
//        const refs=refFileUrls.length
//        ?await Promise.all(
//        refFileUrls.map((fileUrl)=>uploadFile(fileUrl,refreshToken))
//        )
//        :[];
//
//        // 创建会话
//        const convId=await createConversation("新会话",refreshToken);
//
//        // 请求流
//        const{deviceId,token}=await acquireToken(refreshToken);
//        const result=await axios.post(
//        `https://yuewen.cn/api/proto.chat.v1.ChatMessageService/SendMessageStream`,
//        messagesPrepare(convId,messages,refs),
//        {
//        headers:{
//        "Content-Type":"application/connect+json",
//        Cookie:generateCookie(deviceId,token),
//        "Oasis-Webid":deviceId,
//        Referer: `https://yuewen.cn/chats/${convId}`,
//        ...FAKE_HEADERS,
//        },
//        // 120秒超时
//        timeout:120000,
//        validateStatus:()=>true,
//        responseType:"stream",
//        }
//        );
//
//        const streamStartTime=util.timestamp();
//        // 创建转换流将消息格式转换为gpt兼容格式
//        return createTransStream(model,convId,result.data,()=>{
//        logger.success(
//        `Stream has completed transfer ${util.timestamp()-streamStartTime}ms`
//        );
//        // 流传输结束后异步移除会话，如果消息不合规，此操作可能会抛出数据库错误异常，请忽略
//        removeConversation(convId,refreshToken).catch((err)=>
//        console.error(err)
//        );
//        });
//        })().catch((err)=>{
//        if(retryCount<MAX_RETRY_COUNT){
//        logger.error(`Stream response error:${err.message}`);
//        logger.warn(`Try again after ${RETRY_DELAY/1000}s...`);
//        return(async()=>{
//        await new Promise((resolve)=>setTimeout(resolve,RETRY_DELAY));
//        return createCompletionStream(
//        model,
//        messages,
//        refreshToken,
//        useSearch,
//        retryCount+1
//        );
//        })();
//        }
//        throw err;
//        });
//        }
//
//        /**
//         * 提取消息中引用的文件URL
//         *
//         * @param messages 参考gpt系列消息格式，多轮对话请完整提供上下文
//         */
//        function extractRefFileUrls(messages:any[]){
//        const urls=[];
//        // 如果没有消息，则返回[]
//        if(!messages.length){
//        return urls;
//        }
//        // 只获取最新的消息
//        const lastMessage=messages[messages.length-1];
//        if(_.isArray(lastMessage.content)){
//        lastMessage.content.forEach((v)=>{
//        if(!_.isObject(v)||!["file","image_url"].includes(v["type"]))return;
//        // step-free-api支持格式
//        if(
//        v["type"]=="file"&&
//        _.isObject(v["file_url"])&&
//        _.isString(v["file_url"]["url"])
//        )
//        urls.push(v["file_url"]["url"]);
//        // 兼容gpt-4-vision-preview API格式
//        else if(
//        v["type"]=="image_url"&&
//        _.isObject(v["image_url"])&&
//        _.isString(v["image_url"]["url"])
//        )
//        urls.push(v["image_url"]["url"]);
//        });
//        }
//        logger.info("本次请求上传："+urls.length+"个文件");
//        return urls;
//        }
//
//        /**
//         * 消息预处理
//         *
//         * 由于接口只取第一条消息，此处会将多条消息合并为一条，实现多轮对话效果
//         * user:旧消息1
//         * assistant:旧消息2
//         * user:新消息
//         *
//         * @param messages 参考gpt系列消息格式，多轮对话请完整提供上下文
//         */
//        function messagesPrepare(convId:string,messages:any[],refs:any[]){
//        // 检查最新消息是否含有"type": "image_url"或"type": "file",如果有则注入消息
//        let latestMessage=messages[messages.length-1];
//        let hasFileOrImage=
//        Array.isArray(latestMessage.content)&&
//        latestMessage.content.some(
//        (v)=>typeof v==="object"&&["file","image_url"].includes(v["type"])
//        );
//        if(hasFileOrImage){
//        let newFileMessage={
//        content:"以上为历史消息，关注以下用户发送的文件和消息",
//        role:"system",
//        };
//        messages.splice(messages.length-1,0,newFileMessage);
//        logger.info("注入提升尾部文件注意力system prompt");
//        }else{
//        let newTextMessage={
//        content:"以上为历史消息，关注以下用户消息",
//        role:"system",
//        };
//        messages.splice(messages.length-1,0,newTextMessage);
//        logger.info("注入提升尾部消息注意力system prompt");
//        }
//
//        const content=
//        messages.reduce((content,message)=>{
//        if(_.isArray(message.content)){
//        return message.content.reduce((_content,v)=>{
//        if(!_.isObject(v)||v["type"]!="text")return _content;
//        return _content+ `${message.role||"user"}:${v["text"]||""}\n`;
//        },content);
//        }
//        return(content+= `${message.role||"user"}:${message.content}\n`);
//        },"")+"assistant:";
//
//        logger.info("\n对话合并：\n"+content);
//        const json=JSON.stringify({
//        chatId:convId,
//        messageInfo:{
//        text:content,
//        attachments:refs.length>0?refs:undefined,
//        },
//        });
//        const data=wrapData(json);
//        return data;
//        }
//
//        /**
//         * 检查请求结果
//         *
//         * @param result 结果
//         * @param refreshToken 用于刷新access_token的refresh_token
//         */
//        function checkResult(result:AxiosResponse,refreshToken:string){
//        if(!result.data)return null;
//        const{code,message}=result.data;
//        if(!_.isString(code))return result.data;
//        if(code=="unauthenticated")accessTokenMap.delete(refreshToken);
//        throw new APIException(EX.API_REQUEST_FAILED, `[请求step失败]:${message}`);
//        }
//
//        /**
//         * 从流接收完整的消息内容
//         *
//         * @param model 模型名称
//         * @param convId 会话ID
//         * @param stream 消息流
//         */
//        async function receiveStream(model:string,convId:string,stream:any){
//        return new Promise((resolve,reject)=>{
//        // 消息初始化
//        const data={
//        id:convId,
//        model,
//        object:"chat.completion",
//        choices:[
//        {
//        index:0,
//        message:{role:"assistant",content:""},
//        finish_reason:"stop",
//        },
//        ],
//        usage:{prompt_tokens:1,completion_tokens:1,total_tokens:2},
//        created:util.unixTimestamp(),
//        };
//        let refContent="";
//        const parser=(buffer:Buffer)=>{
//        const result=_.attempt(()=>JSON.parse(buffer.toString()));
//        if(_.isError(result)){
//        logger.warn(`Error response:${buffer.toString()}`);
//        throw new Error(`Stream response invalid:${result}`);
//        }
//        if(result.error&&result.error.code)
//        data.choices[0].message.content+= `服务暂时不可用，第三方响应错误：[${result.error.code}]${result.error.message}`;
//        else if(result.pipelineEvent){
//        if(
//        result.pipelineEvent.eventSearch&&
//        result.pipelineEvent.eventSearch.results
//        ){
//        refContent=result.pipelineEvent.eventSearch.results.reduce(
//        (str,v)=>{
//        return(str+= `${v.title}-${v.url}\n`);
//        },
//        ""
//        );
//        }
//        }else if(result.textEvent&&result.textEvent.text)
//        data.choices[0].message.content+=result.textEvent.text;
//        else if(result.doneEvent){
//        data.choices[0].message.content=
//        data.choices[0].message.content.replace(
//        /<(web|url|unknown)_[0-9a-zA-Z]+>/g,
//        ""
//        );
//        data.choices[0].message.content+=refContent
//        ? `\n\n搜索结果来自：\n${refContent.replace(/\n$/,"")}`
//        :"";
//        }
//        };
//        let chunk=Buffer.from([]);
//        let temp=Buffer.from([]);
//        // 将流数据传到转换器
//        stream.on("data",(buffer:Buffer)=>{
//        // 接收数据头
//        chunk=Buffer.concat([temp,chunk,buffer]);
//        if(chunk.length< 5)return;
//        // 读取当前数据块大小
//        const chunkSize=chunk.readUint32BE(1);
//        // 根据当前大小接收完整数据块
//        temp=chunk.subarray(chunkSize+5);
//        chunk=chunk.subarray(0,chunkSize+5);
//        if(chunk.length<chunkSize +5)return;
//        parser(chunk.subarray(5));
//        chunk=Buffer.from([]);
//        });
//        stream.once("error",(err)=>reject(err));
//        stream.once("close",()=>resolve(data));
//        });
//        }
//
//        /**
//         * 创建转换流
//         *
//         * 将流格式转换为gpt兼容流格式
//         *
//         * @param model 模型名称
//         * @param convId 会话ID
//         * @param stream 消息流
//         * @param endCallback 传输结束回调
//         */
//        function createTransStream(
//        model:string,
//        convId:string,
//        stream:any,
//        endCallback?:Function
//        ){
//        // 消息创建时间
//        const created=util.unixTimestamp();
//        // 创建转换流
//        const transStream=new PassThrough();
//        !transStream.closed&&
//        transStream.write(
//        `data:${JSON.stringify({
//        id:convId,
//        model,
//        object:"chat.completion.chunk",
//        choices:[
//        {
//        index:0,
//        delta:{role:"assistant",content:""},
//        finish_reason:null,
//        },
//        ],
//        created,
//        })}\n\n`
//        );
//        const parser=(buffer:Buffer)=>{
//        const result=_.attempt(()=>JSON.parse(buffer.toString()));
//        if(_.isError(result))
//        throw new Error(`Stream response invalid:${result}`);
//        if(result.error&&result.error.code){
//        const data= `data:${JSON.stringify({
//        id:convId,
//        model,
//        object:"chat.completion.chunk",
//        choices:[
//        {
//        index:0,
//        delta:{
//        content: `服务暂时不可用，第三方响应错误：[${result.error.code}]${result.error.message}`,
//        },
//        finish_reason:"stop",
//        },
//        ],
//        usage:{prompt_tokens:1,completion_tokens:1,total_tokens:2},
//        created,
//        })}\n\n`;
//        !transStream.closed&&transStream.write(data);
//        !transStream.closed&&transStream.end("data: [DONE]\n\n");
//        endCallback&&endCallback();
//        }else if(result.pipelineEvent){
//        if(
//        result.pipelineEvent.eventSearch&&
//        result.pipelineEvent.eventSearch.results
//        ){
//        const refContent=result.pipelineEvent.eventSearch.results.reduce(
//        (str,v)=>{
//        return(str+= `检索 ${v.title}-${v.url}...\n`);
//        },
//        ""
//        );
//        const data= `data:${JSON.stringify({
//        id:convId,
//        model,
//        object:"chat.completion.chunk",
//        choices:[
//        {
//        index:0,
//        delta:{
//        content: `${refContent}\n`,
//        },
//        finish_reason:null,
//        },
//        ],
//        created,
//        })}\n\n`;
//        !transStream.closed&&transStream.write(data);
//        }
//        }else if(result.textEvent&&result.textEvent.text){
//        const data= `data:${JSON.stringify({
//        id:convId,
//        model,
//        object:"chat.completion.chunk",
//        choices:[
//        {
//        index:0,
//        delta:{content:result.textEvent.text},
//        finish_reason:null,
//        },
//        ],
//        created,
//        })}\n\n`;
//        !transStream.closed&&transStream.write(data);
//        }else if(result.doneEvent){
//        const data= `data:${JSON.stringify({
//        id:convId,
//        model,
//        object:"chat.completion.chunk",
//        choices:[
//        {
//        index:0,
//        delta:{},
//        finish_reason:"stop",
//        },
//        ],
//        usage:{prompt_tokens:1,completion_tokens:1,total_tokens:2},
//        created,
//        })}\n\n`;
//        !transStream.closed&&transStream.write(data);
//        !transStream.closed&&transStream.end("data: [DONE]\n\n");
//        endCallback&&endCallback();
//        }
//        };
//        let chunk=Buffer.from([]);
//        let temp=Buffer.from([]);
//        // 将流数据传到转换器
//        stream.on("data",(buffer:Buffer)=>{
//        // 接收数据头
//        chunk=Buffer.concat([temp,chunk,buffer]);
//        if(chunk.length< 5)return;
//        // 读取当前数据块大小
//        const chunkSize=chunk.readUint32BE(1);
//        // 根据当前大小接收完整数据块
//        temp=chunk.subarray(chunkSize+5);
//        chunk=chunk.subarray(0,chunkSize+5);
//        if(chunk.length<chunkSize +5)return;
//        parser(chunk.subarray(5));
//        chunk=Buffer.from([]);
//        });
//        stream.once(
//        "error",
//        ()=>!transStream.closed&&transStream.end("data: [DONE]\n\n")
//        );
//        stream.once(
//        "close",
//        ()=>!transStream.closed&&transStream.end("data: [DONE]\n\n")
//        );
//        return transStream;
//        }
//
//        /**
//         * 构建数据包
//         *
//         * @param json 需要发送的JSON字符串
//         */
//        function wrapData(json:string){
//        const data=Buffer.from(json);
//        const buffer=Buffer.alloc(data.length+5);
//        buffer.set(data,5);
//        const dataView=new DataView(
//        buffer.buffer,
//        buffer.byteOffset,
//        buffer.byteLength
//        );
//        dataView.setUint8(0,0x00);
//        dataView.setUint32(1,data.length);
//        return buffer;
//        }
//
//        /**
//         * 生成cookie
//         */
//        function generateCookie(deviceId:string,accessToken:string){
//        return[`Oasis-Token=${accessToken}`, `Oasis-Webid=${deviceId}`].join("; ");
//        }
//
//        /**
//         * 预检查文件URL有效性
//         *
//         * @param fileUrl 文件URL
//         */
//        async function checkFileUrl(fileUrl:string){
//        if(util.isBASE64Data(fileUrl))return;
//        const result=await axios.head(fileUrl,{
//        timeout:15000,
//        headers:{
//        UserAgent:
//        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
//        },
//        validateStatus:()=>true,
//        });
//        if(result.status>=400)
//        throw new APIException(
//        EX.API_FILE_URL_INVALID,
//        `File ${fileUrl}is not valid:[${result.status}]${result.statusText}`
//        );
//        // 检查文件大小
//        if(result.headers&&result.headers["content-length"]){
//        const fileSize=parseInt(result.headers["content-length"],10);
//        if(fileSize>FILE_MAX_SIZE)
//        throw new APIException(
//        EX.API_FILE_EXECEEDS_SIZE,
//        `File ${fileUrl}is not valid`
//        );
//        }
//        }
//
//        /**
//         * 上传文件
//         *
//         * @param fileUrl 文件URL
//         * @param refreshToken 用于刷新access_token的refresh_token
//         */
//        async function uploadFile(fileUrl:string,refreshToken:string){
//        // 预检查远程文件URL可用性
//        await checkFileUrl(fileUrl);
//
//        let filename,fileData:Buffer,mimeType;
//        // 如果是BASE64数据则直接转换为Buffer
//        if(util.isBASE64Data(fileUrl)){
//        mimeType=util.extractBASE64DataFormat(fileUrl);
//        const ext=mime.getExtension(mimeType);
//        filename= `${util.uuid()}.${ext}`;
//        fileData=Buffer.from(util.removeBASE64DataHeader(fileUrl),"base64");
//        }
//        // 下载文件到内存，如果您的服务器内存很小，建议考虑改造为流直传到下一个接口上，避免停留占用内存
//        else{
//        filename=path.basename(fileUrl);
//        const queryIndex=filename.indexOf("?");
//        if(queryIndex!=-1)filename=filename.substring(0,queryIndex);
//        ({data:fileData}=await axios.get(fileUrl,{
//        responseType:"arraybuffer",
//        // 100M限制
//        maxContentLength:FILE_MAX_SIZE,
//        headers:{
//        UserAgent:
//        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
//        },
//        // 60秒超时
//        timeout:60000,
//        }));
//        }
//
//        // 获取文件的MIME类型
//        mimeType=mimeType||mime.getType(filename);
//        // 上传文件到目标OSS
//        const{deviceId,token}=await acquireToken(refreshToken);
//        let result=await axios.request({
//        method:"PUT",
//        url: `https://yuewen.cn/api/storage?file_name=${filename}`,
//        data:fileData,
//        // 100M限制
//        maxBodyLength:FILE_MAX_SIZE,
//        // 60秒超时
//        timeout:60000,
//        headers:{
//        "Content-Type":mimeType,
//        Cookie:generateCookie(deviceId,token),
//        "Oasis-Webid":deviceId,
//        Referer:"https://yuewen.cn/chats/new",
//        "Stepchat-Meta-Width":"undefined",
//        "Stepchat-Meta-Height":"undefined",
//        "Stepchat-Meta-Size": `${fileData.byteLength}`,
//        ...FAKE_HEADERS,
//        },
//        validateStatus:()=>true,
//        });
//        const{id:fileId}=checkResult(result,refreshToken);
//
//        let fileStatus,
//        needFurtherCall=true;
//        const startTime=util.unixTimestamp();
//        while(needFurtherCall){
//        // 获取文件上传结果
//        result=await axios.post(
//        "https://yuewen.cn/api/proto.file.v1.FileService/GetFileStatus",
//        {
//        id:fileId,
//        },
//        {
//        headers:{
//        Cookie:generateCookie(deviceId,token),
//        "Oasis-Webid":deviceId,
//        Referer:"https://yuewen.cn/chats/new",
//        ...FAKE_HEADERS,
//        },
//        timeout:15000,
//        }
//        );
//        ({fileStatus,needFurtherCall}=checkResult(result,refreshToken));
//        // 上传失败处理
//        if([12,22,59,404].includes(fileStatus))
//        throw new APIException(EX.API_FILE_UPLOAD_FAILED);
//        // 上传超时处理
//        if(util.unixTimestamp()-startTime>60)
//        throw new APIException(EX.API_FILE_UPLOAD_TIMEOUT);
//        }
//        await new Promise((resolve)=>setTimeout(resolve,5000));
//
//        return{
//        attachmentType:mimeType,
//        attachmentId:fileId,
//        name:filename,
//        width:"undefined",
//        height:"undefined",
//        size: `${fileData.byteLength}`,
//        };
//        }
//
//        /**
//         * Token切分
//         *
//         * @param authorization 认证字符串
//         */
//        function tokenSplit(authorization:string){
//        return authorization.replace("Bearer ","").split(",");
//        }
//
//        /**
//         * 获取Token存活状态
//         */
//        async function getTokenLiveStatus(refreshToken:string){
//        const[deviceId,token]=refreshToken.split("@");
//        const result=await axios.post(
//        "https://yuewen.cn/passport/proto.api.passport.v1.PassportService/RegisterDevice",
//        {},
//        {
//        headers:{
//        Cookie: `Oasis-Token=${token}`,
//        Referer:"https://yuewen.cn/chats/new",
//        ...FAKE_HEADERS,
//        "Oasis-Webid":deviceId,
//        },
//        timeout:15000,
//        validateStatus:()=>true,
//        }
//        );
//        try{
//        const{
//        accessToken:{raw:accessTokenRaw},
//        refreshToken:{raw:refreshTokenRaw},
//        }=checkResult(result,refreshToken);
//        return!!(accessTokenRaw&&refreshTokenRaw);
//        }catch(err){
//        return false;
//        }
//        }
//
//        export default {
//        createConversation,
//        createCompletion,
//        createCompletionStream,
//        getTokenLiveStatus,
//        tokenSplit,
//        };
//
//        }