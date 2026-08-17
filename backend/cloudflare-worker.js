export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return new Response("Sadia backend is running.", { status: 200 });
    }

    try {
      const body = await request.json();
      const userMessage = body.message || "";
      const memoryContext = body.memory_context || "";

      const systemPrompt =
        "তুমি Sadia, একজন Bangla-speaking AI সহকারী। ব্যবহারকারীর নাম Kolija। " +
        "তুমি যত্নশীল, বন্ধুত্বপূর্ণ, বুদ্ধিমান এবং স্বাভাবিকভাবে কথা বলো। " +
        "উত্তর সংক্ষিপ্ত ও স্বাভাবিক রাখো, রোবটিক না। " +
        (memoryContext ? `ব্যবহারকারী সম্পর্কে যা মনে রাখা আছে:\n${memoryContext}` : "");

      const geminiResponse = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": env.GEMINI_API_KEY
          },
          body: JSON.stringify({
            contents: [
              {
                role: "user",
                parts: [{ text: `${systemPrompt}\n\nব্যবহারকারী: ${userMessage}` }]
              }
            ]
          })
        }
      );

      const data = await geminiResponse.json();
      const reply =
        data?.candidates?.[0]?.content?.parts?.[0]?.text ||
        "দুঃখিত Kolija, এই মুহূর্তে উত্তর দিতে পারছি না।";

      return new Response(JSON.stringify({ reply }), {
        headers: { "Content-Type": "application/json" }
      });
    } catch (err) {
      return new Response(
        JSON.stringify({ reply: "Kolija, একটা সমস্যা হয়েছে। আবার চেষ্টা করো।" }),
        { headers: { "Content-Type": "application/json" }, status: 200 }
      );
    }
  }
};
