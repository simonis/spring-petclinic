build-PetClinicHttpHandler:
	mkdir -p $(ARTIFACTS_DIR)/lib
	# Use 'cd ${PWD}' to build from the top-level project directory instead of building from the
	# current tmp-directory '$(shell pwd)' created by SAM (see https://github.com/aws/aws-sam-cli/issues/4571).
	bash -c "cd ${PWD} && mvn -Dmaven.compiler.debug=true clean spring-javaformat:apply package"
	bash -c "cd ${PWD} && cp ./target/spring-petclinic*.jar $(ARTIFACTS_DIR)/lib/"
