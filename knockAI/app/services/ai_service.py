import os
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()


class AIService:
    def __init__(self):
        self.api_key = os.getenv("OPENAI_API_KEY")
        self.model = os.getenv("GPT_MODEL", "gpt-4o-mini")
        self.client = OpenAI(api_key=self.api_key) if self.api_key else None

    def analyze_news(self, news_content: str) -> str:
        """뉴스 분석"""
        if not self.client:
            return "AI 서비스가 설정되지 않았습니다."

        prompt = (
            "다음 뉴스 기사를 분석하고 요약해주세요. "
            "핵심 내용을 간단히 정리하고, 주가에 미칠 영향을 분석해주세요:\n\n" + news_content
        )
        return self._generate_response(prompt)

    def analyze_portfolio(self, portfolio_summary: str, user_investment_style: str = None) -> str:
        """포트폴리오 분석"""
        if not self.client:
            return "AI 서비스가 설정되지 않았습니다."

        style_text = f"사용자 투자 스타일: {user_investment_style}\n\n" if user_investment_style else ""
        
        prompt = (
            f"다음 포트폴리오를 종합적으로 분석해주세요:\n\n{portfolio_summary}\n\n"
            f"{style_text}"
            "다음 항목들을 포함하여 분석해주세요:\n"
            "1. 포트폴리오 건강도 평가 (1-10점)\n"
            "2. 리스크 분석 (높음/중간/낮음)\n"
            "3. 종목 간 상관관계 분석\n"
            "4. 비중 과다 종목 경고\n"
            "5. 리밸런싱 제안\n"
            "6. 투자 스타일에 맞는 개선 방안"
        )
        return self._generate_response(prompt)

    def answer_question_with_context(
        self, question: str, conversation_history: str = None, history_count: int = 0
    ) -> str:
        """문맥을 포함한 질문 답변"""
        if not self.client:
            return "AI 서비스가 설정되지 않았습니다."

        messages = []
        
        system_prompt = "당신은 전문 주식 투자 분석가입니다. 정확하고 명확한 분석을 제공해주세요."
        if history_count > 0:
            system_prompt += "\n\n이전 대화 기록을 참고하여 연속적인 대화를 이어가세요."
        
        messages.append({"role": "system", "content": system_prompt})

        # 이전 대화 기록 추가
        if history_count > 0 and conversation_history:
            lines = conversation_history.split("\n")
            for line in lines:
                if line.startswith("user: ") or line.startswith("사용자: "):
                    content = line.split(": ", 1)[1] if ": " in line else line
                    messages.append({"role": "user", "content": content})
                elif line.startswith("assistant: ") or line.startswith("AI: "):
                    content = line.split(": ", 1)[1] if ": " in line else line
                    messages.append({"role": "assistant", "content": content})

        # 현재 질문 추가
        messages.append({"role": "user", "content": question})

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0.7,
                max_tokens=1000
            )
            return response.choices[0].message.content
        except Exception as e:
            return f"AI 분석 중 오류가 발생했습니다: {str(e)}"

    def _generate_response(self, prompt: str) -> str:
        """기본 응답 생성"""
        if not self.client:
            return "AI 서비스가 설정되지 않았습니다."

        messages = [
            {
                "role": "system",
                "content": "당신은 전문 주식 투자 분석가입니다. 정확하고 명확한 분석을 제공해주세요."
            },
            {"role": "user", "content": prompt}
        ]

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=0.7,
                max_tokens=1000
            )
            return response.choices[0].message.content
        except Exception as e:
            return f"AI 분석 중 오류가 발생했습니다: {str(e)}"







