package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberDto {

    private String username;

    private int age;

    public MemberDto() {}

    @QueryProjection //이 어노테이션을 붙이고 gradle/other/compileQuerydsl을 눌러주면 Q파일이 생성된다.

    /** compileQuerydsl을 누르면 이런게 뜨는데 정상이다.

     * Note: Running JPAAnnotationProcessor
     * Note: Serializing Entity types
     * Note: Generating study.querydsl.domain.QMember for [study.querydsl.domain.Member]
     * Note: Generating study.querydsl.domain.QHello for [study.querydsl.domain.Hello]
     * Note: Generating study.querydsl.domain.QTeam for [study.querydsl.domain.Team]
     * Note: Serializing Projection types
     * Note: Generating study.querydsl.dto.QMemberDto for [study.querydsl.dto.MemberDto]
     * Note: Running JPAAnnotationProcessor
     * Note: Running JPAAnnotationProcessor
     */

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}