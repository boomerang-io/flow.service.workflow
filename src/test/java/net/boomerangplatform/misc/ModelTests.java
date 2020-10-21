package net.boomerangplatform.misc;

import org.junit.Test;
import com.openpojo.reflection.filters.FilterPackageInfo;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;

public class ModelTests {

  private static final String[] packages = {"net.boomerangplatform.model","net.boomerangplatform.mongo.entity",
      "net.boomerangplatform.model.profile"};

  @Test
  public void verifyExternalServiceModels() {

    for (String packge : packages) {
      Validator validator = ValidatorBuilder.create()

          .with(new GetterMustExistRule()).with(new SetterMustExistRule()).with(new SetterTester())
          .with(new GetterTester()).build();

      validator.validate(packge, new FilterPackageInfo());
    }

  }
}
