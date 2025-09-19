package com.example.bankcards.util;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Random;

@IdGeneratorType(CardNumberGenerator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SixteenDigitId {

}

/**
 * Генератор идентификаторов для полей аннотированных {@link SixteenDigitId}.
 * <p>
 * Генерирует строку длиной 16 символов, первым символом которой всегда является число от 1 до 9.
 */
class CardNumberGenerator implements IdentifierGenerator {
	private static final Random random = new Random();
	
	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) {
		StringBuilder builder = new StringBuilder();
		
		// first number must be from 1 to 9
		builder.append(random.nextInt(9) + 1);
		
		for (int i = 0; i < 15; i++) {
			builder.append(random.nextInt(10));
		}
		
		return builder.toString();
	}
}
