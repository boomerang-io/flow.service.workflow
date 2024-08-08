package io.boomerang.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.enums.ref.ParamType;
import io.boomerang.model.ref.ParamSpec;
import io.boomerang.model.ref.RunParam;

public class ParameterUtil {
  
  /*
   * Add a parameter to an existing Run Parameter list
   * 
   * @param the parameter list
   * 
   * @param the new parameter to add
   * 
   * @return the parameter list
   */
  public static List<RunParam> paramSpecToRunParam(List<ParamSpec> parameterList) {
    return parameterList.stream().map(p -> new RunParam(p.getName(), p.getDefaultValue(),
        p.getType() != null ? p.getType() : ParamType.string)).collect(Collectors.toList());
  }

  /*
   * Add a parameter to an existing Run Parameter list
   * 
   * @param the parameter list
   * 
   * @param the new parameter to add
   * 
   * @return the parameter list
   */
  public static List<RunParam> addUniqueParam(List<RunParam> parameterList, RunParam param) {
    if (parameterList.stream().noneMatch(p -> param.getName().equals(p.getName()))) {
      parameterList.add(param);
    } else {
      parameterList.stream().filter(p -> param.getName().equals(p.getName())).findFirst()
          .ifPresent(p -> p.setValue(param.getValue()));
    }
    return parameterList;
  }

  /*
   * Add a Run Parameter List to an existing Run Parameter list ensuring unique names
   * 
   * @param the parameter list
   * 
   * @param the new parameter to add
   * 
   * @return the parameter list
   */
  public static List<RunParam> addUniqueParams(List<RunParam> origParameterList,
      List<RunParam> newParameterList) {
    newParameterList.stream().forEach(p -> {
      addUniqueParam(origParameterList, p);
    });
    return origParameterList;
  }

  /*
   * Add a parameter to an existing Run Parameter list
   * 
   * @param the parameter list
   * 
   * @param the new parameter spec to add
   * 
   * @return the parameter list
   */
  public static List<ParamSpec> addUniqueParamSpec(List<ParamSpec> parameterList, ParamSpec param) {
    if (parameterList.stream().noneMatch(p -> param.getName().equals(p.getName()))) {
      parameterList.add(param);
    } else {
      parameterList.stream().filter(p -> param.getName().equals(p.getName())).findFirst()
          .ifPresent(p -> {
            p.setDefaultValue(param.getDefaultValue());
            p.setDescription(param.getDescription());
            p.setType(param.getType());
          });
    }
    return parameterList;
  }

  /*
   * Add a ParamSpec Parameter List to an existing ParamSpec Parameter list ensuring unique names
   * 
   * @param the parameter list
   * 
   * @param the new parameter to add
   * 
   * @return the parameter list
   */
  public static List<ParamSpec> addUniqueParamSpecs(List<ParamSpec> origParameterList,
      List<ParamSpec> newParameterList) {
    newParameterList.stream().forEach(p -> {
      addUniqueParamSpec(origParameterList, p);
    });
    return origParameterList;
  }

  /*
   * Converts a Parameter Map to a Run Parameter List. This allows us to go between the two object
   * types for storing Run Parameters
   * 
   * @param the parameter map
   * 
   * @return the parameter list
   */
  public static List<RunParam> mapToRunParamList(Map<String, Object> parameterMap) {
    List<RunParam> parameterList = new LinkedList<>();
    if (parameterMap != null) {
      for (Entry<String, Object> entry : parameterMap.entrySet()) {
        String key = entry.getKey();
        ParamType type = ParamType.object;
        if (parameterMap.get(key) instanceof String) {
          type = ParamType.string;
        } else if (parameterMap.get(key) instanceof List) {
          type = ParamType.array;
        }  
        RunParam param = new RunParam(key, parameterMap.get(key), type);
        parameterList.add(param);
      }
    }
    return parameterList;
  }

  /*
   * Converts a Run Parameter List to a Parameter Map. This allows us to go between the two object
   * types for storing Run Parameters
   * 
   * @param the parameter map
   * 
   * @return the parameter list
   */
  public static Map<String, Object> runParamListToMap(List<RunParam> parameterList) {
    Map<String, Object> parameterMap = new HashMap<>();
    if (parameterList != null) {
      parameterList.stream().forEach(p -> {
        parameterMap.put(p.getName(), p.getValue());
      });
    }
    return parameterMap;
  }

  /*
   * Checks the Run Parameter list for a matching name
   * 
   * @param the parameter list
   * 
   * @param the name of the parameter
   * 
   * @return boolean
   */
  public static boolean containsName(List<RunParam> parameterList, String name) {
    return parameterList.stream().anyMatch(p -> name.equals(p.getName()));
  }

  /*
   * Retrieve the value for the matching name in Run Parameter list
   * 
   * @param the parameter list
   * 
   * @param the name of the parameter
   * 
   * @return the value
   */
  public static Object getValue(List<RunParam> parameterList, String name) {
    Object value = null;
    Optional<RunParam> param =
        parameterList.stream().filter(p -> name.equals(p.getName())).findFirst();
    if (param.isPresent()) {
      value = param.get().getValue();
    }
    return value;
  }

  /*
   * Remove the entry for the matching name in Run Parameter list
   * 
   * @param the parameter list
   * 
   * @param the name of the parameter
   * 
   * @return the reduced list
   */
  public static List<RunParam> removeEntry(List<RunParam> parameterList, String name) {
    List<RunParam> reducedParamList = new LinkedList<>();
    reducedParamList =
        parameterList.stream().filter(p -> !name.equals(p.getName())).collect(Collectors.toList());
    return reducedParamList;
  }

  /*
   * Turns the AbstractParam used by the UI into ParamSpec used by the Engine and Handlers
   * 
   * TODO: what is the mapping of ConfigType to ParamType
   */
  public static List<ParamSpec> abstractParamsToParamSpecs(List<AbstractParam> abstractParams,
      List<ParamSpec> paramSpecs) {
    List<ParamSpec> params = new LinkedList<>();
    if (abstractParams != null && !abstractParams.isEmpty()) {
      for (AbstractParam ap : abstractParams) {
        ParamSpec param = new ParamSpec();
        if (paramSpecs != null && !paramSpecs.isEmpty() && paramSpecs.stream().anyMatch(p -> p.getName().equals(ap.getKey()))) {
          param =
              paramSpecs.stream().filter(p -> p.getName().equals(ap.getKey())).findFirst().get();
          paramSpecs.remove(param);
        } else {
          param.setName(ap.getKey());
        }
        param.setDefaultValue(ap.getDefaultValue());
        param.setDescription(ap.getDescription());
        param.setType(ParamType.string);
        params.add(param);
      };
    }
    // If any paramSpecs are remaining, return them
    if (paramSpecs != null && !paramSpecs.isEmpty()) {
      params.addAll(paramSpecs);
    }
    return params;
  }
  
  //Similar to above, however assumes that the Abstract Param is source of truth.
  //That means if there is no defaultValue or description in the Abstract Param, then null is the correct value to set
  //If the Abstract Param doesn't exist but it does in the ParamSpec, then do not return it, it must have been deleted.
  public static List<ParamSpec> abstractParamsToParamSpecsV2(List<AbstractParam> abstractParams,
      List<ParamSpec> paramSpecs) {
    List<ParamSpec> params = new LinkedList<>();
    if (abstractParams != null && !abstractParams.isEmpty()) {
      for (AbstractParam ap : abstractParams) {
        ParamSpec param = new ParamSpec();
        //We mainly do this for secret abstractParams where the value would have been set to the UI as blank
        if (paramSpecs != null && !paramSpecs.isEmpty() && paramSpecs.stream().anyMatch(p -> p.getName().equals(ap.getKey()))) {
          param =
              paramSpecs.stream().filter(p -> p.getName().equals(ap.getKey())).findFirst().get();
          paramSpecs.remove(param);
        } else {
          param.setName(ap.getKey());
        }
        param.setName(ap.getKey());
        //If secret, only replace the stored value when not empty (as its removed on display to the UI)
        if (ap.getType().equals("secret") && !Objects.isNull(ap.getDefaultValue()) && !ap.getDefaultValue().isEmpty()) {          
          param.setDefaultValue(ap.getDefaultValue());
        }
        param.setDescription(ap.getDescription());
        //TODO: conditionally know the type based on AbstractParam type
        // Need to do a mapping
        param.setType(ParamType.string);
        params.add(param);
      };
    }
    return params;
  }

  /*
   * Turns the ParamSpec into an AbstractParam. Used if the Workflow was created by API or other
   * means not the UI
   * 
   * TODO: what is the mapping of ConfigType to ParamType
   */
  public static List<AbstractParam> paramSpecToAbstractParam(List<ParamSpec> paramSpecs,
      List<AbstractParam> abstractParams) {
    List<AbstractParam> params = new LinkedList<>();
    if (paramSpecs != null && !paramSpecs.isEmpty()) {
      for (ParamSpec ps : paramSpecs) {
        AbstractParam param = new AbstractParam();
        if (abstractParams.stream().anyMatch(p -> p.getKey().equals(ps.getName()))) {
          param = abstractParams.stream().filter(p -> p.getKey().equals(ps.getName())).findFirst()
              .get();
          abstractParams.remove(param);
        } else {
          param.setKey(ps.getName());
        }
        if (ps.getDefaultValue() != null) {          
          param.setDefaultValue(ps.getDefaultValue().toString());
        }
        param.setDescription(ps.getDescription());
        //TOOD better type mapping
        if (param.getType().isEmpty()) {
          param.setType("text");
        }
        params.add(param);
      }
    }
    // If any abstractParams are remaining, return them
    if (abstractParams != null && !abstractParams.isEmpty()) {
      params.addAll(abstractParams);
    }
    return params;
  }
  
  //Similar to above, however assumes that the Param spec has the correct value
  public static List<AbstractParam> paramSpecToAbstractParamV2(List<ParamSpec> paramSpecs,
      List<AbstractParam> abstractParams) {
    //If paramSpecs is empty, return empty params
    List<AbstractParam> params = new LinkedList<>();
    if (paramSpecs != null && !paramSpecs.isEmpty()) {
      for (ParamSpec ps : paramSpecs) {
        AbstractParam param = new AbstractParam();
        if (abstractParams.stream().anyMatch(p -> p.getKey().equals(ps.getName()))) {
          param = abstractParams.stream().filter(p -> p.getKey().equals(ps.getName())).findFirst()
              .get();
          abstractParams.remove(param);
        } else {
          param.setKey(ps.getName());
        }
        // Any value from Param Spec is trusted, thus override even if empty / null
        param.setDefaultValue(Objects.isNull(ps.getDefaultValue()) ? null : ps.getDefaultValue().toString());
        param.setDescription(ps.getDescription());
        //Check before defaulting as may be an existing property and because the following elements don't exist on ParamSpec, we have to merge.
        if (Objects.isNull(param.getLabel()) || param.getLabel().isEmpty()) {          
          param.setLabel(ps.getName());
        }
        if (Objects.isNull(param.getType()) || param.getType().isEmpty()) {     
          //TOOD better type mapping
          //Would have to detect multiline values
          //Handle objects vs arrays
          param.setType("text");
        }
        if (Objects.isNull(param.isReadOnly())) {
            param.setReadOnly(false);
        }
        if (Objects.isNull(param.getPlaceholder()) || param.getPlaceholder().isEmpty()) {          
          param.setPlaceholder("");
        }
        params.add(param);
      };
    }
    return params;
  }
}
