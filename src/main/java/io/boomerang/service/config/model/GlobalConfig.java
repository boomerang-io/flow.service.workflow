package io.boomerang.service.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.boomerang.mongo.entity.FlowGlobalConfigEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalConfig extends FlowGlobalConfigEntity {

}
