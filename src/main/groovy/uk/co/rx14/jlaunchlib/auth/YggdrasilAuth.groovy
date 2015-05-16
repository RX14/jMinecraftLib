package uk.co.rx14.jlaunchlib.auth

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import static uk.co.rx14.jlaunchlib.auth.MinecraftAuthResult.Profile

public class YggdrasilAuth implements MinecraftAuth {

	@Override
	MinecraftAuthResult auth(CredentialsProvider provider) {
		if (!provider) throw new IllegalArgumentException("CredentialsProvider is null")
		Credentials credentials = provider.ask();
		if (!credentials) throw new IllegalArgumentException("Credentials are null")

		def res = request("authenticate", [
			agent   : [
				name   : "Minecraft",
				version: 1
			],
			username: credentials.username,
			password: credentials.password
		])

		if (res.error) {
			if (res.error == "ForbiddenOperationException") {
				throw new ForbiddenOperationException(res.errorMessage)
			} else if (res.error == "IllegalArgumentException") {
				throw new IllegalArgumentException(res.errorMessage)
			} else {
				throw new RuntimeException(res.errorMessage)
			}
		}

		new MinecraftAuthResult(
			accessToken: res.accessToken,
			clientToken: res.clientToken,
			selectedProfile: new Profile(
				name: res.selectedProfile.name,
				id: res.selectedProfile.id
			),
			valid: true
		)
	}

	@Override
	MinecraftAuthResult refresh(MinecraftAuthResult previous) {
		if (!previous.valid) throw new IllegalArgumentException("MinecraftAuthResult is not valid")

		def res = request("refresh", [
			accessToken: previous.accessToken,
			clientToken: previous.clientToken
		])

		if (res.error) {
			if (res.error == "ForbiddenOperationException" && res.errorMessage == "Invalid token.") {
				return new MinecraftAuthResult(
					accessToken: previous.accessToken,
					clientToken: previous.clientToken,
					selectedProfile: previous.selectedProfile,
					valid: false
				)
			} else if (res.error == "ForbiddenOperationException") {
				throw new ForbiddenOperationException((String) res.errorMessage)
			} else if (res.error == "IllegalArgumentException") {
				throw new IllegalArgumentException((String) res.errorMessage)
			} else {
				throw new RuntimeException((String) res.errorMessage)
			}
		}

		new MinecraftAuthResult(
			accessToken: res.accessToken,
			clientToken: res.clientToken,
			selectedProfile: new Profile(
				name: res.selectedProfile.name,
				id: res.selectedProfile.id
			),
			valid: true
		)
	}

	def request(String path, body) {
		def jsonBody = JsonOutput.toJson(body)

		println jsonBody

		HttpResponse<String> response =
			Unirest.post("https://authserver.mojang.com/$path")
			       .header("Content-Type", "application/json")
			       .body(jsonBody)
			       .asString();

		println response.body

		new JsonSlurper().parseText(response.body)
	}
}
