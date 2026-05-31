package com.dian.processor;

import com.dian.annotation.RemoteRepository;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * 收集所有 {@link RemoteRepository} 标注的实现类，生成 {@code com.dian.generated.RepoModule}：
 *
 * <pre>
 * public final class RepoModule {
 *     public static void init() {
 *         RepoRegistry.register(DataRepo.class, DataRepoImpl::new);
 *     }
 * }
 * </pre>
 *
 * App 启动时调用 RepoModule.init() 即可把实现注册进 com.common.ui.RepoRegistry，
 * BaseViewModel 通过 repo&lt;T&gt;() 取用，无需反射、无需 lib 反向依赖 app。
 */
@AutoService(Processor.class)
public class RemoteRepoProcessor extends AbstractProcessor {

    private static final ClassName REPO_REGISTRY =
            ClassName.get("com.common.ui", "RepoRegistry");

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        messager = env.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(RemoteRepository.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(RemoteRepository.class);
        if (elements.isEmpty()) {
            return false;
        }

        MethodSpec.Builder init = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class);

        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "@Repo 只能标注在类上", element);
                continue;
            }
            TypeElement impl = (TypeElement) element;
            ClassName implName = ClassName.get(impl);
            for (TypeMirror iface : resolveInterfaces(impl)) {
                // RepoRegistry.register(Iface.class, Impl::new);
                init.addStatement("$T.register($T.class, $T::new)",
                        REPO_REGISTRY, TypeName.get(iface), implName);
            }
        }

        TypeSpec repoModule = TypeSpec.classBuilder("RepoModule")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                .addMethod(init.build())
                .build();

        try {
            JavaFile.builder("com.dian.generated", repoModule)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "生成 RepoModule 失败: " + e.getMessage());
        }
        return true;
    }

    /** 取 @Repo(value)；未显式指定时退化为该类直接实现的全部接口，仍无接口则绑定到类自身。 */
    private List<? extends TypeMirror> resolveInterfaces(TypeElement impl) {
        List<? extends TypeMirror> declared = readRepoValue(impl);
        if (!declared.isEmpty()) {
            return declared;
        }
        List<? extends TypeMirror> interfaces = impl.getInterfaces();
        if (!interfaces.isEmpty()) {
            return interfaces;
        }
        return Collections.singletonList(impl.asType());
    }

    private List<? extends TypeMirror> readRepoValue(TypeElement impl) {
        try {
            // 读取 Class<?>[] 会抛 MirroredTypesException，从异常里拿 TypeMirror
            impl.getAnnotation(RemoteRepository.class).value();
            return Collections.emptyList();
        } catch (MirroredTypesException e) {
            return e.getTypeMirrors();
        }
    }
}