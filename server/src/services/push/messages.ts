// Textes des notifications (FR/EN/HE), portés à l'identique depuis
// supabase/functions/notify/index.ts pour garantir la parité de contenu.

export type PushEvent =
  | "threshold"
  | "complete"
  | "distribute_prompt"
  | "distributed"
  | "selection_reminder"
  | "final_reminder"
  | "selection_extended"
  | "auto_extended"
  | "deleted";

export interface PushMessage {
  title: string;
  body: string;
}

type Lang = "fr" | "en" | "he";

function langOf(locale: string): Lang {
  const l = (locale || "fr").toLowerCase();
  if (l.startsWith("en")) return "en";
  // L'hébreu peut arriver sous son ancien code ISO « iw ».
  if (l.startsWith("he") || l.startsWith("iw")) return "he";
  return "fr";
}

export function messageFor(
  event: string,
  value: number | null,
  chainName: string,
  locale: string,
): PushMessage {
  const lang = langOf(locale);
  const pick = (fr: PushMessage, en: PushMessage, he: PushMessage): PushMessage =>
    lang === "en" ? en : lang === "he" ? he : fr;

  if (event === "threshold") {
    return pick(
      { title: "Chaîne de Tehilim", body: `« ${chainName} » est complétée à ${value} %` },
      { title: "Tehilim chain", body: `“${chainName}” is ${value}% complete` },
      { title: "שרשרת תהילים", body: `«${chainName}» הושלמה ב־${value}%` },
    );
  }
  if (event === "distributed") {
    return pick(
      { title: "Chaîne distribuée 🙏", body: `« ${chainName} » a été distribuée — bonne lecture` },
      { title: "Chain distributed 🙏", body: `“${chainName}” has been distributed — happy reading` },
      { title: "השרשרת חולקה 🙏", body: `«${chainName}» חולקה — קריאה נעימה` },
    );
  }
  if (event === "complete") {
    return pick(
      { title: "Chaîne complétée 🎉", body: `« ${chainName} » — les 150 Tehilim sont attribués !` },
      { title: "Chain complete 🎉", body: `“${chainName}” — all 150 Tehilim are assigned!` },
      { title: "השרשרת הושלמה 🎉", body: `«${chainName}» — כל 150 הפרקים הוקצו!` },
    );
  }
  if (event === "distribute_prompt") {
    return pick(
      { title: "À toi de distribuer 🙏", body: `« ${chainName} » est complète — distribue-la pour lancer la lecture` },
      { title: "Your turn to distribute 🙏", body: `“${chainName}” is complete — distribute it to start the reading` },
      { title: "תורכם לחלק 🙏", body: `«${chainName}» מלאה — חלקו אותה כדי להתחיל את הקריאה` },
    );
  }
  if (event === "selection_reminder") {
    return pick(
      { title: "Sélection bientôt close", body: `« ${chainName} » : il reste ${value} Tehilim à prendre` },
      { title: "Selection closing soon", body: `“${chainName}”: ${value} Tehilim left to pick` },
      { title: "הבחירה נסגרת בקרוב", body: `«${chainName}»: נותרו ${value} פרקים לבחירה` },
    );
  }
  if (event === "final_reminder") {
    return pick(
      { title: "Dernière chance ⏳", body: `« ${chainName} » ferme très bientôt — ${value} Tehilim encore libres` },
      { title: "Last chance ⏳", body: `“${chainName}” closes very soon — ${value} Tehilim still free` },
      { title: "הזדמנות אחרונה ⏳", body: `«${chainName}» נסגרת ממש בקרוב — ${value} פרקים עוד פנויים` },
    );
  }
  if (event === "selection_extended") {
    return pick(
      { title: "Sélection prolongée ⏰", body: `« ${chainName} » : plus de temps pour choisir vos Tehilim` },
      { title: "Selection extended ⏰", body: `“${chainName}”: more time to pick your Tehilim` },
      { title: "הבחירה הוארכה ⏰", body: `«${chainName}»: עוד זמן לבחור את הפרקים שלכם` },
    );
  }
  if (event === "auto_extended") {
    return pick(
      { title: "Sélection prolongée de 3 h ⏰", body: `« ${chainName} » : ${value} Tehilim encore libres — 3 h de plus pour choisir` },
      { title: "Selection extended +3h ⏰", body: `“${chainName}”: ${value} Tehilim still free — 3 extra hours to pick` },
      { title: "הבחירה הוארכה ב־3 שעות ⏰", body: `«${chainName}»: ${value} פרקים עוד פנויים — עוד 3 שעות לבחירה` },
    );
  }
  return pick(
    { title: "Chaîne supprimée", body: `« ${chainName} » a été supprimée par son créateur` },
    { title: "Chain deleted", body: `“${chainName}” was deleted by its creator` },
    { title: "השרשרת נמחקה", body: `«${chainName}» נמחקה על ידי יוצרה` },
  );
}
