/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.javaaidev.openai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.StreamOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@JsonInclude(Include.NON_NULL)
public class OpenAiChatOptions implements ToolCallingChatOptions {
    @JsonProperty("model")
    private String model;
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;
    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;
    @JsonProperty("logprobs")
    private Boolean logprobs;
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;
    @JsonProperty("n")
    private Integer n;
    @JsonProperty("modalities")
    private List<String> outputModalities;
    @JsonProperty("audio")
    private OpenAiApi.ChatCompletionRequest.AudioParameters outputAudio;
    @JsonProperty("presence_penalty")
    private Double presencePenalty;
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    @JsonProperty("stream_options")
    private OpenAiApi.ChatCompletionRequest.StreamOptions streamOptions;
    @JsonProperty("seed")
    private Integer seed;
    @JsonProperty("stop")
    private List<String> stop;
    @JsonProperty("temperature")
    private Double temperature;
    @JsonProperty("top_p")
    private Double topP;
    @JsonProperty("tools")
    private List<OpenAiApi.FunctionTool> tools;
    @JsonProperty("tool_choice")
    private Object toolChoice;
    @JsonProperty("user")
    private String user;
    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;
    @JsonProperty("store")
    private Boolean store;
    @JsonProperty("metadata")
    private Map<String, String> metadata;
    @JsonProperty("reasoning_effort")
    private String reasoningEffort;
    @JsonIgnore
    private List<ToolCallback> toolCallbacks = new ArrayList();
    @JsonIgnore
    private Set<String> toolNames = new HashSet();
    @JsonIgnore
    private Boolean internalToolExecutionEnabled;
    @JsonIgnore
    private Map<String, String> httpHeaders = new HashMap();
    @JsonIgnore
    private Map<String, Object> toolContext = new HashMap();

    public OpenAiChatOptions() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static OpenAiChatOptions fromOptions(OpenAiChatOptions fromOptions) {
        return builder().model(fromOptions.getModel()).frequencyPenalty(fromOptions.getFrequencyPenalty()).logitBias(fromOptions.getLogitBias()).logprobs(fromOptions.getLogprobs()).topLogprobs(fromOptions.getTopLogprobs()).maxTokens(fromOptions.getMaxTokens()).maxCompletionTokens(fromOptions.getMaxCompletionTokens()).N(fromOptions.getN()).outputModalities(fromOptions.getOutputModalities() != null ? new ArrayList(fromOptions.getOutputModalities()) : null).outputAudio(fromOptions.getOutputAudio()).presencePenalty(fromOptions.getPresencePenalty()).responseFormat(fromOptions.getResponseFormat()).streamUsage(fromOptions.getStreamUsage()).seed(fromOptions.getSeed()).stop(fromOptions.getStop() != null ? new ArrayList(fromOptions.getStop()) : null).temperature(fromOptions.getTemperature()).topP(fromOptions.getTopP()).tools(fromOptions.getTools()).toolChoice(fromOptions.getToolChoice()).user(fromOptions.getUser()).parallelToolCalls(fromOptions.getParallelToolCalls()).toolCallbacks(fromOptions.getToolCallbacks() != null ? new ArrayList(fromOptions.getToolCallbacks()) : null).toolNames(fromOptions.getToolNames() != null ? new HashSet(fromOptions.getToolNames()) : null).httpHeaders(fromOptions.getHttpHeaders() != null ? new HashMap(fromOptions.getHttpHeaders()) : null).internalToolExecutionEnabled(fromOptions.getInternalToolExecutionEnabled()).toolContext(fromOptions.getToolContext() != null ? new HashMap(fromOptions.getToolContext()) : null).store(fromOptions.getStore()).metadata(fromOptions.getMetadata()).reasoningEffort(fromOptions.getReasoningEffort()).build();
    }

    public Boolean getStreamUsage() {
        return this.streamOptions != null;
    }

    public void setStreamUsage(Boolean enableStreamUsage) {
        this.streamOptions = enableStreamUsage ? StreamOptions.INCLUDE_USAGE : null;
    }

    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getFrequencyPenalty() {
        return this.frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Map<String, Integer> getLogitBias() {
        return this.logitBias;
    }

    public void setLogitBias(Map<String, Integer> logitBias) {
        this.logitBias = logitBias;
    }

    public Boolean getLogprobs() {
        return this.logprobs;
    }

    public void setLogprobs(Boolean logprobs) {
        this.logprobs = logprobs;
    }

    public Integer getTopLogprobs() {
        return this.topLogprobs;
    }

    public void setTopLogprobs(Integer topLogprobs) {
        this.topLogprobs = topLogprobs;
    }

    public Integer getMaxTokens() {
        return this.maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getMaxCompletionTokens() {
        return this.maxCompletionTokens;
    }

    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public Integer getN() {
        return this.n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public List<String> getOutputModalities() {
        return this.outputModalities;
    }

    public void setOutputModalities(List<String> modalities) {
        this.outputModalities = modalities;
    }

    public OpenAiApi.ChatCompletionRequest.AudioParameters getOutputAudio() {
        return this.outputAudio;
    }

    public void setOutputAudio(OpenAiApi.ChatCompletionRequest.AudioParameters audio) {
        this.outputAudio = audio;
    }

    public Double getPresencePenalty() {
        return this.presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public ResponseFormat getResponseFormat() {
        return this.responseFormat;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    public OpenAiApi.ChatCompletionRequest.StreamOptions getStreamOptions() {
        return this.streamOptions;
    }

    public void setStreamOptions(OpenAiApi.ChatCompletionRequest.StreamOptions streamOptions) {
        this.streamOptions = streamOptions;
    }

    public Integer getSeed() {
        return this.seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    @JsonIgnore
    public List<String> getStopSequences() {
        return this.getStop();
    }

    @JsonIgnore
    public void setStopSequences(List<String> stopSequences) {
        this.setStop(stopSequences);
    }

    public List<String> getStop() {
        return this.stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public Double getTemperature() {
        return this.temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return this.topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public List<OpenAiApi.FunctionTool> getTools() {
        return this.tools;
    }

    public void setTools(List<OpenAiApi.FunctionTool> tools) {
        this.tools = tools;
    }

    public Object getToolChoice() {
        return this.toolChoice;
    }

    public void setToolChoice(Object toolChoice) {
        this.toolChoice = toolChoice;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Boolean getParallelToolCalls() {
        return this.parallelToolCalls;
    }

    public void setParallelToolCalls(Boolean parallelToolCalls) {
        this.parallelToolCalls = parallelToolCalls;
    }

    @JsonIgnore
    public List<ToolCallback> getToolCallbacks() {
        return this.toolCallbacks;
    }

    @JsonIgnore
    public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
        Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
        Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
        this.toolCallbacks = toolCallbacks;
    }

    @JsonIgnore
    public Set<String> getToolNames() {
        return this.toolNames;
    }

    @JsonIgnore
    public void setToolNames(Set<String> toolNames) {
        Assert.notNull(toolNames, "toolNames cannot be null");
        Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
        toolNames.forEach((tool) -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
        this.toolNames = toolNames;
    }

    @Nullable
    @JsonIgnore
    public Boolean getInternalToolExecutionEnabled() {
        return this.internalToolExecutionEnabled;
    }

    @JsonIgnore
    public void setInternalToolExecutionEnabled(@Nullable Boolean internalToolExecutionEnabled) {
        this.internalToolExecutionEnabled = internalToolExecutionEnabled;
    }

    public Map<String, String> getHttpHeaders() {
        return this.httpHeaders;
    }

    public void setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    @JsonIgnore
    public Integer getTopK() {
        return null;
    }

    @JsonIgnore
    public Map<String, Object> getToolContext() {
        return this.toolContext;
    }

    @JsonIgnore
    public void setToolContext(Map<String, Object> toolContext) {
        this.toolContext = toolContext;
    }

    public Boolean getStore() {
        return this.store;
    }

    public void setStore(Boolean store) {
        this.store = store;
    }

    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getReasoningEffort() {
        return this.reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    public OpenAiChatOptions copy() {
        return fromOptions(this);
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.model, this.frequencyPenalty, this.logitBias, this.logprobs, this.topLogprobs, this.maxTokens, this.maxCompletionTokens, this.n, this.presencePenalty, this.responseFormat, this.streamOptions, this.seed, this.stop, this.temperature, this.topP, this.tools, this.toolChoice, this.user, this.parallelToolCalls, this.toolCallbacks, this.toolNames, this.httpHeaders, this.internalToolExecutionEnabled, this.toolContext, this.outputModalities, this.outputAudio, this.store, this.metadata, this.reasoningEffort});
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            OpenAiChatOptions other = (OpenAiChatOptions)o;
            return Objects.equals(this.model, other.model) && Objects.equals(this.frequencyPenalty, other.frequencyPenalty) && Objects.equals(this.logitBias, other.logitBias) && Objects.equals(this.logprobs, other.logprobs) && Objects.equals(this.topLogprobs, other.topLogprobs) && Objects.equals(this.maxTokens, other.maxTokens) && Objects.equals(this.maxCompletionTokens, other.maxCompletionTokens) && Objects.equals(this.n, other.n) && Objects.equals(this.presencePenalty, other.presencePenalty) && Objects.equals(this.responseFormat, other.responseFormat) && Objects.equals(this.streamOptions, other.streamOptions) && Objects.equals(this.seed, other.seed) && Objects.equals(this.stop, other.stop) && Objects.equals(this.temperature, other.temperature) && Objects.equals(this.topP, other.topP) && Objects.equals(this.tools, other.tools) && Objects.equals(this.toolChoice, other.toolChoice) && Objects.equals(this.user, other.user) && Objects.equals(this.parallelToolCalls, other.parallelToolCalls) && Objects.equals(this.toolCallbacks, other.toolCallbacks) && Objects.equals(this.toolNames, other.toolNames) && Objects.equals(this.httpHeaders, other.httpHeaders) && Objects.equals(this.toolContext, other.toolContext) && Objects.equals(this.internalToolExecutionEnabled, other.internalToolExecutionEnabled) && Objects.equals(this.outputModalities, other.outputModalities) && Objects.equals(this.outputAudio, other.outputAudio) && Objects.equals(this.store, other.store) && Objects.equals(this.metadata, other.metadata) && Objects.equals(this.reasoningEffort, other.reasoningEffort);
        } else {
            return false;
        }
    }

    public String toString() {
        return "OpenAiChatOptions: " + ModelOptionsUtils.toJsonString(this);
    }

    public static class Builder {
        protected OpenAiChatOptions options;

        public Builder() {
            this.options = new OpenAiChatOptions();
        }

        public Builder(OpenAiChatOptions options) {
            this.options = options;
        }

        public Builder model(String model) {
            this.options.model = model;
            return this;
        }

        public Builder model(OpenAiApi.ChatModel openAiChatModel) {
            this.options.model = openAiChatModel.getName();
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.options.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder logitBias(Map<String, Integer> logitBias) {
            this.options.logitBias = logitBias;
            return this;
        }

        public Builder logprobs(Boolean logprobs) {
            this.options.logprobs = logprobs;
            return this;
        }

        public Builder topLogprobs(Integer topLogprobs) {
            this.options.topLogprobs = topLogprobs;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.options.maxTokens = maxTokens;
            return this;
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.options.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public Builder N(Integer n) {
            this.options.n = n;
            return this;
        }

        public Builder outputModalities(List<String> modalities) {
            this.options.outputModalities = modalities;
            return this;
        }

        public Builder outputAudio(OpenAiApi.ChatCompletionRequest.AudioParameters audio) {
            this.options.outputAudio = audio;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.options.presencePenalty = presencePenalty;
            return this;
        }

        public Builder responseFormat(ResponseFormat responseFormat) {
            this.options.responseFormat = responseFormat;
            return this;
        }

        public Builder streamUsage(boolean enableStreamUsage) {
            this.options.streamOptions = enableStreamUsage ? StreamOptions.INCLUDE_USAGE : null;
            return this;
        }

        public Builder seed(Integer seed) {
            this.options.seed = seed;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.options.stop = stop;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.options.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.options.topP = topP;
            return this;
        }

        public Builder tools(List<OpenAiApi.FunctionTool> tools) {
            this.options.tools = tools;
            return this;
        }

        public Builder toolChoice(Object toolChoice) {
            this.options.toolChoice = toolChoice;
            return this;
        }

        public Builder user(String user) {
            this.options.user = user;
            return this;
        }

        public Builder parallelToolCalls(Boolean parallelToolCalls) {
            this.options.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            this.options.setToolCallbacks(toolCallbacks);
            return this;
        }

        public Builder toolCallbacks(ToolCallback... toolCallbacks) {
            Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
            this.options.toolCallbacks.addAll(Arrays.asList(toolCallbacks));
            return this;
        }

        public Builder toolNames(Set<String> toolNames) {
            Assert.notNull(toolNames, "toolNames cannot be null");
            this.options.setToolNames(toolNames);
            return this;
        }

        public Builder toolNames(String... toolNames) {
            Assert.notNull(toolNames, "toolNames cannot be null");
            this.options.toolNames.addAll(Set.of(toolNames));
            return this;
        }

        public Builder internalToolExecutionEnabled(@Nullable Boolean internalToolExecutionEnabled) {
            this.options.setInternalToolExecutionEnabled(internalToolExecutionEnabled);
            return this;
        }

        public Builder httpHeaders(Map<String, String> httpHeaders) {
            this.options.httpHeaders = httpHeaders;
            return this;
        }

        public Builder toolContext(Map<String, Object> toolContext) {
            if (this.options.toolContext == null) {
                this.options.toolContext = toolContext;
            } else {
                this.options.toolContext.putAll(toolContext);
            }

            return this;
        }

        public Builder store(Boolean store) {
            this.options.store = store;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.options.metadata = metadata;
            return this;
        }

        public Builder reasoningEffort(String reasoningEffort) {
            this.options.reasoningEffort = reasoningEffort;
            return this;
        }

        public OpenAiChatOptions build() {
            return this.options;
        }
    }
}

