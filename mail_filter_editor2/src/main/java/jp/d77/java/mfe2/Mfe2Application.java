package jp.d77.java.mfe2;

import java.util.Optional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Mfe2Application {
	private static String[] staticArgs;

	public static void main(String[] args) {
		staticArgs = args;
		SpringApplication.run(Mfe2Application.class, args);
	}

	public static Optional<String> getFilePath(){
		if ( Mfe2Application.staticArgs == null ) return Optional.empty();
		if ( Mfe2Application.staticArgs.length <= 0 ) return Optional.empty();
		return Optional.ofNullable( Mfe2Application.staticArgs[0] );
	}
}
