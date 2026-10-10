@testable import SampleData
import Testing

struct SampleItemTests {
    @Test func aShortBodyIsItsOwnPreview() {
        let item = SampleItem(id: 1, title: "One", body: "Short\nand sweet")
        #expect(item.preview == "Short and sweet")
    }

    @Test func aLongBodyIsCutAtAWord() {
        let body = String(repeating: "word ", count: 40)
        let preview = SampleItem(id: 1, title: "One", body: body).preview
        #expect(preview.hasSuffix("word…"))
        #expect(preview.count <= 91)
    }

    @Test func theWireFormatLosesItsStrayWhitespace() {
        let dto = SampleItemDTO(id: 7, title: "  Seven\n", body: "\nBody ")
        #expect(dto.item == SampleItem(id: 7, title: "Seven", body: "Body"))
    }
}
