import {
	fetchKilocodeNotificationsCore,
	fetchKilocodeNotificationsHandler,
} from "../webviewMessageHandlerUtils"

describe("webviewMessageHandlerUtils (standalone v5 notifications disabled)", () => {
	it("returns empty notifications without remote fetching or v7 promotion banners", async () => {
		const result = await fetchKilocodeNotificationsCore("test-token", [], undefined)

		expect(result).toEqual({
			notifications: [],
			nativeNotifications: [],
		})
	})

	it("posts an empty kilocodeNotificationsResponse list from fetchKilocodeNotificationsHandler", async () => {
		const postMessageToWebview = vi.fn().mockResolvedValue(undefined)
		const mockProvider = {
			postMessageToWebview,
		} as any

		await fetchKilocodeNotificationsHandler(mockProvider)

		expect(postMessageToWebview).toHaveBeenCalledTimes(1)
		expect(postMessageToWebview).toHaveBeenCalledWith({
			type: "kilocodeNotificationsResponse",
			notifications: [],
		})
	})
})
