package net.boomerangplatform.service.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.boomerangplatform.mongo.entity.FlowGlobalConfigEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalConfig extends FlowGlobalConfigEntity {

}
