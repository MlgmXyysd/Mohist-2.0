package red.mohist.common.asm.remap.remappers;

import net.md_5.specialsource.CustomRemapper;
import net.md_5.specialsource.NodeType;
import red.mohist.common.asm.remap.ASMUtils;
import red.mohist.common.asm.remap.model.ClassMapping;

import java.util.Iterator;
import java.util.Map;

/**
 * 负责nsm->mcp的remap
 *
 * @author pyz
 * @date 2019/7/3 10:38 PM
 */
public class MohistJarRemapper extends CustomRemapper {

    public final MohistJarMapping jarMapping;

    @Override
    public String mapSignature(String signature, boolean typeSignature) {
        if (ASMUtils.isValidSingnature(signature)) {
            return super.mapSignature(signature, typeSignature);
        } else {
            return signature;
        }
    }

    public MohistJarRemapper(MohistJarMapping jarMapping) {
        this.jarMapping = jarMapping;
    }

    @Override
    public String map(String typeName) {
        return mapTypeName(typeName, jarMapping.packages, jarMapping.byNMSSrcName, typeName);
    }

    public static String mapTypeName(String typeName, Map<String, String> packageMap, Map<String, ClassMapping> classMap, String defaultIfUnmapped) {
        String mapped = mapClassName(typeName, packageMap, classMap);
        return mapped != null ? mapped : defaultIfUnmapped;
    }

    /**
     * Helper method to map a class name by package (prefix) or class (exact)
     */
    private static String mapClassName(String className, Map<String, String> packageMap, Map<String, ClassMapping> classMap) {
        if (classMap != null && classMap.containsKey(className)) {
            ClassMapping mapping = classMap.get(className);
            return mapping.getMcpSrcName();
        }

        int index = className.lastIndexOf('$');
        if (index != -1) {
            String outer = className.substring(0, index);
            String mapped = mapClassName(outer, packageMap, classMap);
            if (mapped == null) return null;
            return mapped + className.substring(index);
        }

        if (packageMap != null) {
            Iterator<String> iter = packageMap.keySet().iterator();
            while (iter.hasNext()) {
                String oldPackage = iter.next();
                if (matchClassPackage(oldPackage, className)) {
                    String newPackage = packageMap.get(oldPackage);

                    return moveClassPackage(newPackage, getSimpleName(oldPackage, className));
                }
            }
        }

        return null;
    }

    private static boolean matchClassPackage(String packageName, String className) {
        if (packageName.equals(".")) {
            return isDefaultPackage(className);
        }

        return className.startsWith(packageName);
    }

    private static String moveClassPackage(String packageName, String classSimpleName) {
        if (packageName.equals(".")) {
            return classSimpleName;
        }

        return packageName + classSimpleName;
    }

    private static boolean isDefaultPackage(String className) {
        return className.indexOf('/') == -1;
    }

    private static String getSimpleName(String oldPackage, String className) {
        if (oldPackage.equals(".")) {
            return className;
        }

        return className.substring(oldPackage.length());
    }

    @Override
    public String mapFieldName(String owner, String name, String desc, int access) {
        String mapped = jarMapping.tryClimb(jarMapping.fields, NodeType.FIELD, owner, name, access);
        return mapped == null ? name : mapped;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc, int access) {
        String mapped = jarMapping.tryClimb(jarMapping.methods, NodeType.METHOD, owner, name + " " + desc, access);
        return mapped == null ? name : mapped;
    }

}
