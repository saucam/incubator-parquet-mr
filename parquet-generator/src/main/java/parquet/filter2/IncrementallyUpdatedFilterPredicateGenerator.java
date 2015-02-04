package parquet.filter2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class IncrementallyUpdatedFilterPredicateGenerator {

  public static void main(String[] args) throws IOException {
    File srcFile = new File(args[0] + "/parquet/filter2/recordlevel/IncrementallyUpdatedFilterPredicateBuilder.java");
    srcFile = srcFile.getAbsoluteFile();
    File parent = srcFile.getParentFile();
    if (!parent.exists()) {
      if (!parent.mkdirs()) {
        throw new IOException("Couldn't mkdirs for " + parent);
      }
    }
    new IncrementallyUpdatedFilterPredicateGenerator(srcFile).run();
  }

  private final FileWriter writer;

  public IncrementallyUpdatedFilterPredicateGenerator(File file) throws IOException {
    this.writer = new FileWriter(file);
  }

  private static class TypeInfo {
    public final String className;
    public final String primitiveName;
    public final boolean useComparable;
    public final boolean supportsInequality;

    private TypeInfo(String className, String primitiveName, boolean useComparable, boolean supportsInequality) {
      this.className = className;
      this.primitiveName = primitiveName;
      this.useComparable = useComparable;
      this.supportsInequality = supportsInequality;
    }
  }

  private static final TypeInfo[] TYPES = new TypeInfo[]{
    new TypeInfo("Integer", "int", false, true),
    new TypeInfo("Long", "long", false, true),
    new TypeInfo("Boolean", "boolean", false, false),
    new TypeInfo("Float", "float", false, true),
    new TypeInfo("Double", "double", false, true),
    new TypeInfo("Binary", "Binary", true, true),
  };

  public void run() throws IOException {
    add("package parquet.filter2.recordlevel;\n" +
        "\n" +
        "import parquet.common.schema.ColumnPath;\n" +
        "import parquet.filter2.predicate.Operators.Eq;\n" +
        "import parquet.filter2.predicate.Operators.Gt;\n" +
        "import parquet.filter2.predicate.Operators.GtEq;\n" +
        "import parquet.filter2.predicate.Operators.LogicalNotUserDefined;\n" +
        "import parquet.filter2.predicate.Operators.Lt;\n" +
        "import parquet.filter2.predicate.Operators.LtEq;\n" +
        "import parquet.filter2.predicate.Operators.NotEq;\n" +
        "import parquet.filter2.predicate.Operators.UserDefined;\n" +
        "import parquet.filter2.predicate.UserDefinedPredicate;\n" +
        "import parquet.filter2.recordlevel.IncrementallyUpdatedFilterPredicate.ValueInspector;\n" +
        "import parquet.io.api.Binary;\n\n" +
        "/**\n" +
        " * This class is auto-generated by {@link parquet.filter2.IncrementallyUpdatedFilterPredicateGenerator}\n" +
        " * Do not manually edit!\n" +
        " * See {@link IncrementallyUpdatedFilterPredicateBuilderBase}\n" +
        " */\n");

    add("public class IncrementallyUpdatedFilterPredicateBuilder extends IncrementallyUpdatedFilterPredicateBuilderBase {\n\n");

    addVisitBegin("Eq");
    for (TypeInfo info : TYPES) {
      addEqNotEqCase(info, true);
    }
    addVisitEnd();

    addVisitBegin("NotEq");
    for (TypeInfo info : TYPES) {
      addEqNotEqCase(info, false);
    }
    addVisitEnd();

    addVisitBegin("Lt");
    for (TypeInfo info : TYPES) {
      addInequalityCase(info, "<");
    }
    addVisitEnd();

    addVisitBegin("LtEq");
    for (TypeInfo info : TYPES) {
      addInequalityCase(info, "<=");
    }
    addVisitEnd();

    addVisitBegin("Gt");
    for (TypeInfo info : TYPES) {
      addInequalityCase(info, ">");
    }
    addVisitEnd();

    addVisitBegin("GtEq");
    for (TypeInfo info : TYPES) {
      addInequalityCase(info, ">=");
    }
    addVisitEnd();

    add("  @Override\n" +
        "  public <T extends Comparable<T>, U extends UserDefinedPredicate<T>> IncrementallyUpdatedFilterPredicate visit(UserDefined<T, U> pred) {\n");
    addUdpBegin();
    for (TypeInfo info : TYPES) {
      addUdpCase(info, false);
    }
    addVisitEnd();

    add("  @Override\n" +
        "  public <T extends Comparable<T>, U extends UserDefinedPredicate<T>> IncrementallyUpdatedFilterPredicate visit(LogicalNotUserDefined<T, U> notPred) {\n" +
        "    UserDefined<T, U> pred = notPred.getUserDefined();\n");
    addUdpBegin();
    for (TypeInfo info : TYPES) {
      addUdpCase(info, true);
    }
    addVisitEnd();

    add("}\n");
    writer.close();
  }

  private void addVisitBegin(String inVar) throws IOException {
    add("  @Override\n" +
        "  public <T extends Comparable<T>> IncrementallyUpdatedFilterPredicate visit(" + inVar + "<T> pred) {\n" +
        "    ColumnPath columnPath = pred.getColumn().getColumnPath();\n" +
        "    Class<T> clazz = pred.getColumn().getColumnType();\n" +
        "\n" +
        "    ValueInspector valueInspector = null;\n\n");
  }

  private void addVisitEnd() throws IOException {
    add("    if (valueInspector == null) {\n" +
        "      throw new IllegalArgumentException(\"Encountered unknown type \" + clazz);\n" +
        "    }\n" +
        "\n" +
        "    addValueInspector(columnPath, valueInspector);\n" +
        "    return valueInspector;\n" +
        "  }\n\n");
  }

  private void addEqNotEqCase(TypeInfo info, boolean isEq) throws IOException {
    add("    if (clazz.equals(" + info.className + ".class)) {\n" +
        "      if (pred.getValue() == null) {\n" +
        "        valueInspector = new ValueInspector() {\n" +
        "          @Override\n" +
        "          public void updateNull() {\n" +
        "            setResult(" + isEq + ");\n" +
        "          }\n" +
        "\n" +
        "          @Override\n" +
        "          public void update(" + info.primitiveName + " value) {\n" +
        "            setResult(" + !isEq + ");\n" +
        "          }\n" +
        "        };\n" +
        "      } else {\n" +
        "        final " + info.primitiveName + " target = (" + info.className + ") (Object) pred.getValue();\n" +
        "\n" +
        "        valueInspector = new ValueInspector() {\n" +
        "          @Override\n" +
        "          public void updateNull() {\n" +
        "            setResult(" + !isEq +");\n" +
        "          }\n" +
        "\n" +
        "          @Override\n" +
        "          public void update(" + info.primitiveName + " value) {\n");

    if (info.useComparable) {
      add("            setResult(" + compareEquality("value", "target", isEq) + ");\n");
    } else {
      add("            setResult(" + (isEq ? "value == target" : "value != target" )  + ");\n");
    }

    add("          }\n" +
        "        };\n" +
        "      }\n" +
        "    }\n\n");
  }

  private void addInequalityCase(TypeInfo info, String op) throws IOException {
    if (!info.supportsInequality) {
      add("    if (clazz.equals(" + info.className + ".class)) {\n");
      add("      throw new IllegalArgumentException(\"Operator " + op + " not supported for " + info.className + "\");\n");
      add("    }\n\n");
      return;
    }

    add("    if (clazz.equals(" + info.className + ".class)) {\n" +
        "      final " + info.primitiveName + " target = (" + info.className + ") (Object) pred.getValue();\n" +
        "\n" +
        "      valueInspector = new ValueInspector() {\n" +
        "        @Override\n" +
        "        public void updateNull() {\n" +
        "          setResult(false);\n" +
        "        }\n" +
        "\n" +
        "        @Override\n" +
        "        public void update(" + info.primitiveName + " value) {\n");

    if (info.useComparable) {
      add("          setResult(value.compareTo(target) " + op + " 0);\n");
    } else {
      add("          setResult(value " + op + " target);\n");
    }
    add("        }\n" +
        "      };\n" +
        "    }\n\n");
  }

  private void addUdpBegin() throws IOException {
    add("    ColumnPath columnPath = pred.getColumn().getColumnPath();\n" +
        "    Class<T> clazz = pred.getColumn().getColumnType();\n" +
        "\n" +
        "    ValueInspector valueInspector = null;\n" +
        "\n" +
        "    final U udp = pred.getUserDefinedPredicate();\n" +
        "\n");
  }

  private void addUdpCase(TypeInfo info, boolean invert)throws IOException {
    add("    if (clazz.equals(" + info.className + ".class)) {\n" +
        "      valueInspector = new ValueInspector() {\n" +
        "        @Override\n" +
        "        public void updateNull() {\n" +
        "          setResult(" + (invert ? "!" : "") + "udp.keep(null));\n" +
        "        }\n" +
        "\n" +
        "        @SuppressWarnings(\"unchecked\")\n" +
        "        @Override\n" +
        "        public void update(" + info.primitiveName + " value) {\n" +
        "          setResult(" + (invert ? "!" : "") + "udp.keep((T) (Object) value));\n" +
        "        }\n" +
        "      };\n" +
        "    }\n\n");
  }

  private String compareEquality(String var, String target, boolean eq) {
    return var + ".compareTo(" + target + ")" + (eq ? " == 0 " : " != 0");
  }

  private void add(String s) throws IOException {
    writer.write(s);
  }
}
