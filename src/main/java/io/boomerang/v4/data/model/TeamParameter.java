package io.boomerang.v4.data.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.mongo.model.AbstractConfigurationProperty;

@JsonInclude(Include.NON_NULL)
public class TeamParameter extends AbstractConfigurationProperty {

}
