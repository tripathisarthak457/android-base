import { Creator } from "../../components/creator.tsx";
import { Faq } from "../../components/faq.tsx";
import { Hero } from "../../components/hero.tsx";
import { Included } from "../../components/included.tsx";
import { Footer, Nav } from "../../components/site-chrome.tsx";
import { StylesShowcase } from "../../components/styles-showcase.tsx";
import { ConfigureIsland } from "./configure-island.tsx";

export default function Home() {
  return (
    <>
      <Nav />
      <main className="min-h-screen">
        <Hero />
        <StylesShowcase />
        <Included />
        <ConfigureIsland />
        <Faq />
        <Creator />
      </main>
      <Footer />
    </>
  );
}
