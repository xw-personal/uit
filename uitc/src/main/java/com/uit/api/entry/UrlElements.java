package com.uit.api.entry;

import java.util.List;
import java.util.Map;

import lombok.Data;


@Data
public class UrlElements<T> {
    private String type;
    
    private String taskId;

    private List<T> components;
    
    @Data
    public static class UIComponent {
        private String id;
        private String type;
        private Map<String,Object> config;

        public UIComponent(String id,String type){
            this.id = id;
            this.type = type;
        }

        public UIComponent(String id,String type,Map<String,Object> config){
            this.id = id;
            this.type = type;
            this.config = config;
        }
    }
}


