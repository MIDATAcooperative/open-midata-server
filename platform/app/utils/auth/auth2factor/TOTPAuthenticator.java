/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.auth.auth2factor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Objects;

import javax.imageio.ImageIO;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.SecretGenerator;
import com.bastiaanjansen.otp.TOTPGenerator;

import io.nayuki.qrcodegen.QrCode;
import models.MidataId;
import models.RateLimitedAction;
import models.User;
import models.enums.AuditEventType;
import models.enums.EMailStatus;
import utils.AccessLog;
import utils.InstanceConfig;
import utils.exceptions.AppException;
import utils.exceptions.BadRequestException;
import utils.exceptions.InternalServerException;


public class TOTPAuthenticator implements Authenticator {

	
	/**
	 * Minimum time between tries 
	 */
	public static final long MIN_TIME_BETWEEN_TRIES = 1000l * 1l;
	
	/**
	 * Allow 5 tries per hour
	 */
	public static final long TIMEFRAME = SMSAuthenticator.SMS_TIMEFRAME;
	public static final int MAX_PER_TIMEFRAME = SMSAuthenticator.MAX_PER_TIMEFRAME;
	
	/**
	 * initialize TOTP authentication
	 * @param user
	 * @throws AppException
	 */
	public void setupAuthentication(User user) throws AppException {
		byte[] secret = SecretGenerator.generate();
		user.totpSecret = secret;
		user.totpStatus = EMailStatus.UNVALIDATED;
		User.set(user._id, "totpSecret", user.totpSecret);
		User.set(user._id, "totpStatus", user.totpStatus);
	}
	
	/**
	 * start new TOTP authentication
	 * @param executor user or appInstance
	 * @param prompt Name of application
	 * @param phone phone number of user
	 * @throws AppException
	 */
	public void startAuthentication(MidataId executor, String prompt, User user) throws AppException {
		if (!RateLimitedAction.doRateLimited(user._id, AuditEventType.USER_AUTHENTICATION, MIN_TIME_BETWEEN_TRIES, MAX_PER_TIMEFRAME, TIMEFRAME)) {
			throw new BadRequestException("error.ratelimit", "Rate limit reached.");
		}
		
	}
	
	private TOTPGenerator generator(User user) {
		TOTPGenerator totp = new TOTPGenerator.Builder(user.totpSecret)
		        .withHOTPGenerator(builder -> {
		            builder.withPasswordLength(6);
		            builder.withAlgorithm(HMACAlgorithm.SHA1);
		        })
		        .withPeriod(Duration.ofSeconds(30))
		        .build();
		return totp;
	}
	
	/**
	 * validate code provided by user
	 * @param executor user or appInstance
	 * @param code token provided by user
	 * @return false if code is wrong.
	 * @throws AppException if code has expired
	 */
	public boolean checkAuthentication(MidataId executor, User user, String code) throws AppException {
		TOTPGenerator totp = generator(user);
		
		String tk2 = code.trim();
		String tk1 = totp.now();
		AccessLog.log("tk1="+tk1+" tk2="+tk2);
		if (!totp.verify(tk2)) {
	      throw new BadRequestException("error.invalid.securitytoken", "Token not correct.");
		} else if (user.totpStatus != EMailStatus.VALIDATED) {
			user.totpStatus = EMailStatus.VALIDATED;
			User.set(user._id, "totpStatus", user.totpStatus);
		}
		return true;
	}
	
	/**
	 * end authentication and delete code from database
	 * @param executor user or appInstance
	 * @throws AppException
	 */
	public void finishAuthentication(MidataId executor, User user) throws AppException {
		
	}
	
	/**
	 * Returns a raster image depicting the specified QR Code, with
	 * the specified module scale, border modules, and module colors.
	 * <p>For example, scale=10 and border=4 means to pad the QR Code with 4 light border
	 * modules on all four sides, and use 10&#xD7;10 pixels to represent each module.
	 * @param qr the QR Code to render (not {@code null})
	 * @param scale the side length (measured in pixels, must be positive) of each module
	 * @param border the number of border modules to add, which must be non-negative
	 * @param lightColor the color to use for light modules, in 0xRRGGBB format
	 * @param darkColor the color to use for dark modules, in 0xRRGGBB format
	 * @return a new image representing the QR Code, with padding and scaling
	 * @throws NullPointerException if the QR Code is {@code null}
	 * @throws IllegalArgumentException if the scale or border is out of range, or if
	 * {scale, border, size} cause the image dimensions to exceed Integer.MAX_VALUE
	 */
	private static BufferedImage toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
		Objects.requireNonNull(qr);
		if (scale <= 0 || border < 0)
			throw new IllegalArgumentException("Value out of range");
		if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
			throw new IllegalArgumentException("Scale or border too large");
		
		BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < result.getHeight(); y++) {
			for (int x = 0; x < result.getWidth(); x++) {
				boolean color = qr.getModule(x / scale - border, y / scale - border);
				result.setRGB(x, y, color ? darkColor : lightColor);
			}
		}
		return result;
	}
	
	public InputStream generateQRCode(User user) throws AppException {
		try {
		TOTPGenerator totp = generator(user);
		URI uri = totp.getURI("MIDATA", user.email);
		QrCode qr0 = QrCode.encodeText(uri.toString(), QrCode.Ecc.MEDIUM);
		AccessLog.log("URL:"+uri.toString());
		BufferedImage img = toImage(qr0, 6, 10, 0xFFFFFF, 0x000000);  // See QrCodeGeneratorDemo
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
	    
	    ImageIO.write(img, "png", os);
		os.close();
		return new ByteArrayInputStream(os.toByteArray());
		} catch (IOException e) {
			AccessLog.logException("QRCode", e);
			throw new InternalServerException("error.internal", "Error during QR Code generation");
		} catch (URISyntaxException e) {
			AccessLog.logException("QRCode", e);
			throw new InternalServerException("error.internal", "Error during QR Code generation");
		}
		
	}
}
